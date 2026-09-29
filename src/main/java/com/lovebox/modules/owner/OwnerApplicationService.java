package com.lovebox.modules.owner;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.notification.Mailer;
import com.lovebox.modules.product.ProductDtos.ReviewDecision;
import com.lovebox.modules.product.Vocab;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.service.UserService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Khách thuê gửi đơn làm Chủ đồ → admin duyệt (role OWNER) hoặc từ chối kèm lý do, khách sửa và gửi lại. */
@Service
@RequiredArgsConstructor
public class OwnerApplicationService {

    private final OwnerApplicationRepository repo;
    private final UserService userService;
    private final Mailer mailer;
    private final com.lovebox.modules.notification.Notifications notifications;

    public record Submit(
            @NotBlank @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
            @NotBlank(message = "Cần địa chỉ lấy / nhận lại đồ") @Size(max = 500) String address,
            @NotBlank @Pattern(regexp = "^\\d{6,20}$", message = "Số tài khoản chỉ gồm 6-20 chữ số") String bankAccount,
            @NotBlank(message = "Chọn ngân hàng nhận tiền") String bankName,
            @NotBlank @Size(min = 20, max = 1000, message = "Giới thiệu 20-1000 ký tự") String intro,
            @AssertTrue(message = "Cần đồng ý điều khoản Chủ đồ") boolean agreed) {}

    public record View(UUID id, UUID userId, String userName, String userEmail, String phone, String address,
                       String bankAccount, String bankName, String intro, String status, String rejectReason,
                       Instant createdAt, Instant reviewedAt) {
        static View of(OwnerApplication a) {
            User u = a.getUser();
            return new View(a.getId(), u.getId(), u.getName(), u.getEmail(), a.getPhone(), a.getAddress(), a.getBankAccount(),
                    a.getBankName(), a.getIntro(), a.getStatus(), a.getRejectReason(), a.getCreatedAt(), a.getReviewedAt());
        }
    }

    /** Đơn gần nhất của mình (null nếu chưa gửi). */
    @Transactional(readOnly = true)
    public View mine(UUID userId) {
        return repo.findFirstByUserIdOrderByCreatedAtDesc(userId).map(View::of).orElse(null);
    }

    @Transactional
    public View submit(UUID userId, Submit r) {
        User u = userService.get(userId);
        if (!User.RENTER.equals(u.getRole())) throw invalid(User.OWNER.equals(u.getRole()) ? "Bạn đã là Chủ đồ" : "Tài khoản này không đăng ký làm Chủ đồ");
        if (repo.existsByUserIdAndStatus(userId, OwnerApplication.PENDING)) throw invalid("Bạn đã có đơn đang chờ duyệt");
        if (!Vocab.BANKS.containsKey(r.bankName())) throw invalid("Ngân hàng không hỗ trợ: " + r.bankName());
        OwnerApplication a = new OwnerApplication();
        a.setUser(u);
        a.setPhone(r.phone());
        a.setAddress(r.address().trim());
        a.setBankAccount(r.bankAccount());
        a.setBankName(r.bankName());
        a.setIntro(r.intro().trim());
        return View.of(repo.save(a));
    }

    @Transactional(readOnly = true)
    public List<View> list(String status) {
        return (status == null || status.isBlank() ? repo.findAllByOrderByCreatedAtDesc() : repo.findByStatusOrderByCreatedAtAsc(status))
                .stream().map(View::of).toList();
    }

    /** Duyệt → role OWNER + điền SĐT/địa chỉ/STK còn trống vào hồ sơ. Từ chối → bắt buộc lý do. */
    @Transactional
    public View review(UUID id, ReviewDecision d) {
        OwnerApplication a = repo.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!OwnerApplication.PENDING.equals(a.getStatus())) throw invalid("Đơn này đã được xử lý");
        if (!d.approve() && (d.reason() == null || d.reason().isBlank())) throw invalid("Cần ghi lý do từ chối");
        User u = a.getUser();
        a.setStatus(d.approve() ? OwnerApplication.APPROVED : OwnerApplication.REJECTED);
        a.setRejectReason(d.approve() ? null : d.reason().trim());
        a.setReviewedAt(Instant.now());
        if (d.approve()) {
            if (User.RENTER.equals(u.getRole())) u.setRole(User.OWNER);
            if (isBlank(u.getPhone())) u.setPhone(a.getPhone());
            if (isBlank(u.getAddress())) u.setAddress(a.getAddress());
            if (isBlank(u.getBankAccount())) { u.setBankAccount(a.getBankAccount()); u.setBankName(a.getBankName()); }
        }
        notifications.push(u.getId(), d.approve() ? "Đơn đăng ký Chủ đồ đã được duyệt, bạn có thể đăng đồ cho thuê"
                : "Đơn đăng ký Chủ đồ chưa được duyệt: " + a.getRejectReason(), "/account?tab=owner");
        mailer.send(u.getEmail(), d.approve() ? "Bạn đã trở thành Chủ đồ Lentique" : "Đơn đăng ký Chủ đồ chưa được duyệt",
                (d.approve() ? "<p>Chúc mừng! Bạn đã có thể đăng đồ cho thuê.</p>"
                        : "<p>Đơn đăng ký Chủ đồ của bạn chưa được duyệt.</p><p>Lý do: " + HtmlUtils.htmlEscape(a.getRejectReason()) + "</p><p>Bạn có thể sửa và gửi lại.</p>")
                        + "<p><a href=\"" + mailer.link("/account?tab=owner") + "\">Mở trang Cho thuê</a></p>");
        return View.of(a);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static AppException invalid(String msg) { return new AppException(ErrorCode.VALIDATION_ERROR, msg); }
}
