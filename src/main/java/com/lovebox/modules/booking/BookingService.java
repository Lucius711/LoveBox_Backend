package com.lovebox.modules.booking;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.booking.BookingDtos.*;
import com.lovebox.modules.notification.Mailer;
import com.lovebox.modules.payment.payos.PayOSClient;
import com.lovebox.modules.product.Vocab;
import com.lovebox.modules.product.Product;
import com.lovebox.modules.product.ProductRepository;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.repository.UserRepository;
import com.lovebox.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private static final Set<String> RENTED_OUT = Set.of("RENTED", "RETURNED", "COMPLETED", "DISPUTED");

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PayOSClient payOSClient;
    private final Mailer mailer;

    /**
     * Giỏ hàng → mỗi món thành 1 booking, cùng checkoutCode.
     * Thành tiền = Σ(giá/ngày × số ngày) + Σ cọc + phí ship (tính 1 lần / đơn).
     */
    @Transactional
    public CheckoutResponse checkout(UUID renterId, CheckoutRequest req) {
        LocalDate today = LocalDate.now();
        User renter = userRepository.getReferenceById(renterId);
        long checkoutCode = ThreadLocalRandom.current().nextLong(1_000_000L, 9_000_000_000_000L);
        List<Booking> created = new ArrayList<>();

        // sắp theo productId để 2 checkout đồng thời luôn khoá theo cùng thứ tự (tránh deadlock)
        List<CartItem> items = req.items().stream().sorted(Comparator.comparing(CartItem::productId)).toList();
        for (CartItem item : items) {
            try {
                RentalRules.validateDates(item.startDate(), item.endDate(), today);
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.BOOKING_INVALID_DATES, e.getMessage());
            }
            Product p = productRepository.findByIdForUpdate(item.productId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!Product.APPROVED.equals(p.getStatus())) throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE, p.getName() + " hiện không cho thuê");
            if (p.getOwner().getId().equals(renterId)) throw new AppException(ErrorCode.BOOKING_OWN_PRODUCT);
            boolean clashInCart = created.stream().anyMatch(b -> b.getProduct().getId().equals(p.getId())
                    && RentalRules.overlaps(b.getStartDate(), b.getEndDate(), item.startDate(), item.endDate()));
            if (clashInCart || bookingRepository.existsConflict(p.getId(),
                    item.startDate().minusDays(RentalRules.BUFFER_DAYS), item.endDate().plusDays(RentalRules.BUFFER_DAYS)))
                throw new AppException(ErrorCode.BOOKING_DATE_CONFLICT, p.getName() + ": " + ErrorCode.BOOKING_DATE_CONFLICT.getMessage());

            Booking b = new Booking();
            b.setCode("LT" + String.format("%08X", ThreadLocalRandom.current().nextInt()));
            b.setCheckoutCode(checkoutCode);
            b.setProduct(p);
            b.setRenter(renter);
            b.setStartDate(item.startDate());
            b.setEndDate(item.endDate());
            b.setDays(RentalRules.days(item.startDate(), item.endDate()));
            b.setRentAmount(p.getRentPricePerDay() * b.getDays());
            b.setDepositAmount(p.deposit());
            b.setShippingFee(created.isEmpty() ? RentalRules.shippingFee(req.deliveryMethod()) : 0);
            b.setTotalAmount(b.getRentAmount() + b.getDepositAmount() + b.getShippingFee());
            b.setPaymentMethod(req.paymentMethod());
            b.setRecipientName(req.recipientName().trim());
            b.setPhone(req.phone());
            b.setAddress(req.address().trim());
            b.setDeliveryMethod(req.deliveryMethod());
            b.setNote(req.note());
            created.add(b);
        }
        try {
            bookingRepository.saveAllAndFlush(created);
        } catch (DataIntegrityViolationException e) {   // EXCLUDE constraint — lưới an toàn cuối
            throw new AppException(ErrorCode.BOOKING_DATE_CONFLICT);
        }

        long total = created.stream().mapToLong(Booking::getTotalAmount).sum();
        if ("PAYOS".equals(req.paymentMethod())) {
            var link = payOSClient.createPaymentLink(checkoutCode, total, "LENTIQUE " + (checkoutCode % 1_000_000));
            if (link == null) throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không tạo được mã QR thanh toán (PayOS chưa cấu hình?). Hãy chọn COD hoặc thử lại.");
            created.forEach(b -> { b.setPayosCheckoutUrl(link.checkoutUrl()); b.setPayosQrCode(link.qrCode()); });
        } else {
            created.forEach(this::notifyOwnerNewBooking);   // QR: báo chủ đồ khi đã nhận tiền (markPaid)
        }
        return toCheckout(created);
    }

    public CheckoutResponse checkoutStatus(UUID renterId, long checkoutCode) {
        List<Booking> list = bookingRepository.findByCheckoutCode(checkoutCode).stream()
                .filter(b -> b.getRenter().getId().equals(renterId)).toList();
        if (list.isEmpty()) throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        return toCheckout(list);
    }

    /**
     * PayOS webhook báo đã nhận tiền → giữ cọc (HOLD). Idempotent.
     * Lưu tài khoản người chuyển (nếu PayOS có trả) để sau này hoàn tiền về đúng tài khoản đó.
     */
    @Transactional
    public void markPaid(long checkoutCode, String payerBin, String payerAccount, String payerBankName) {
        boolean hasPayer = payerBin != null && !payerBin.isBlank() && payerAccount != null && !payerAccount.isBlank();
        bookingRepository.findByCheckoutCode(checkoutCode).forEach(b -> {
            if (hasPayer) {
                b.setRefundBankBin(payerBin);
                b.setRefundBankAccount(payerAccount);
                b.setRefundBankName(payerBankName);
            }
            if ("UNPAID".equals(b.getPaymentStatus()) && !"CANCELLED".equals(b.getStatus())) {
                b.setPaymentStatus("PAID");
                b.setDepositStatus("HELD");
                notifyOwnerNewBooking(b);
            }
        });
    }

    public List<View> mine(UUID renterId) {
        return bookingRepository.findByRenterIdOrderByCreatedAtDesc(renterId).stream().map(this::view).toList();
    }

    public List<View> forOwner(UUID ownerId) {
        return bookingRepository.findByProductOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::view).toList();
    }

    public List<View> all(String status) {
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(b -> status == null || b.getStatus().equals(status)).map(this::view).toList();
    }

    @Transactional
    public View cancelByRenter(UUID renterId, UUID bookingId) {
        Booking b = find(bookingId);
        if (!b.getRenter().getId().equals(renterId)) throw new AppException(ErrorCode.FORBIDDEN);
        if (!"PENDING".equals(b.getStatus())) throw new AppException(ErrorCode.BOOKING_STATUS_TRANSITION_INVALID, "Chỉ huỷ được đơn đang chờ xác nhận");
        apply(b, "CANCELLED", null, null);
        return view(b);
    }

    /** Chủ đồ xử lý đơn của mình; admin xử lý mọi đơn + kiểm định/tranh chấp. */
    @Transactional
    public View changeStatus(UserPrincipal actor, UUID bookingId, StatusChange req) {
        Booking b = find(bookingId);
        boolean admin = actor.isAdmin();
        if (!admin && !b.getProduct().getOwner().getId().equals(actor.getId())) throw new AppException(ErrorCode.FORBIDDEN);
        if (!RentalRules.canTransition(b.getStatus(), req.status(), admin))
            throw new AppException(ErrorCode.BOOKING_STATUS_TRANSITION_INVALID,
                    "Không thể chuyển từ " + b.getStatus() + " sang " + req.status());
        if ("PAYOS".equals(b.getPaymentMethod()) && "UNPAID".equals(b.getPaymentStatus()) && !"CANCELLED".equals(req.status()))
            throw new AppException(ErrorCode.BOOKING_STATUS_TRANSITION_INVALID, "Đơn chưa thanh toán QR, chưa thể xử lý");
        apply(b, req.status(), req.deductionAmount(), req.note());
        return view(b);
    }

    private void apply(Booking b, String to, Long deduction, String note) {
        switch (to) {
            case "RENTED" -> {
                if ("COD".equals(b.getPaymentMethod())) { b.setPaymentStatus("PAID"); b.setDepositStatus("HELD"); }
                b.getProduct().setRentCount(b.getProduct().getRentCount() + 1);
            }
            case "DISPUTED" -> {
                b.setDeductionAmount(Math.min(deduction == null ? 0 : deduction, b.getDepositAmount()));
                User r = b.getRenter();
                r.setTrustScore(Math.max(0, r.getTrustScore() - 20));   // hư hỏng / bùng đồ → trừ uy tín
            }
            case "COMPLETED" -> {
                if (deduction != null) b.setDeductionAmount(Math.min(deduction, b.getDepositAmount()));
                // Kiểm định xong → tạo yêu cầu hoàn cọc (trừ phần hư hỏng), admin chuyển khoản tay rồi bấm xác nhận
                long refund = b.getDepositAmount() - b.getDeductionAmount();
                if (refund > 0) queueRefund(b, refund);
                b.setDepositStatus(refund > 0 ? "REFUNDED" : "FORFEITED");
                mailer.send(b.getProduct().getOwner().getEmail(), "Đơn " + b.getCode() + " đã hoàn tất",
                        "<p>Đơn thuê <b>" + b.getProduct().getName() + "</b> (" + b.getCode() + ") đã kiểm định xong và hoàn tất.</p>"
                                + "<p>Tiền thuê: <b>" + vnd(b.getRentAmount()) + "</b>"
                                + (b.getDeductionAmount() > 0 ? " · Bồi thường hư hỏng: <b>" + vnd(b.getDeductionAmount()) + "</b>" : "") + "</p>");
            }
            case "CANCELLED" -> {
                // Đã trả tiền qua QR → hoàn toàn bộ tự động
                if ("PAID".equals(b.getPaymentStatus()) && "PAYOS".equals(b.getPaymentMethod()))
                    queueRefund(b, b.getTotalAmount());
                if ("PAID".equals(b.getPaymentStatus())) b.setPaymentStatus("REFUNDED");
                if ("HELD".equals(b.getDepositStatus())) b.setDepositStatus("REFUNDED");
            }
            default -> { }
        }
        if (note != null && !note.isBlank()) b.setAdminNote(note.trim());
        b.setStatus(to);
    }

    /**
     * Hoàn tiền làm THỦ CÔNG (an toàn hơn chi tự động): chỉ ghi lại số tiền + STK nhận,
     * admin tự chuyển khoản rồi bấm "Đã chuyển khoản" → {@link #confirmRefund}.
     * STK ưu tiên tài khoản khách đã quét QR; không có (COD) → STK trong hồ sơ người thuê.
     */
    private void queueRefund(Booking b, long amount) {
        if (b.getRefundBankAccount() == null) {
            User r = b.getRenter();
            if (r.getBankAccount() == null || r.getBankAccount().isBlank() || r.getBankName() == null)
                throw new AppException(ErrorCode.REFUND_FAILED,
                        "Chưa có tài khoản nhận hoàn tiền — nhờ khách cập nhật STK & ngân hàng trong Hồ sơ rồi thử lại");
            b.setRefundBankBin(Vocab.BANKS.get(r.getBankName()));
            b.setRefundBankAccount(r.getBankAccount());
            b.setRefundBankName(r.getBankName());
        }
        b.setRefundStatus("PROCESSING");   // = chờ admin chuyển khoản
        b.setRefundAmount(amount);
    }

    /** Admin đã chuyển khoản tay xong → chốt hoàn tiền, báo khách. ref: mã giao dịch ngân hàng (tuỳ chọn). */
    @Transactional
    public View confirmRefund(UUID bookingId, String ref) {
        Booking b = find(bookingId);
        if (!"PROCESSING".equals(b.getRefundStatus()))
            throw new AppException(ErrorCode.BOOKING_STATUS_TRANSITION_INVALID, "Đơn này không có khoản hoàn tiền đang chờ chuyển");
        b.setRefundStatus("SUCCEEDED");
        b.setRefundRef(ref == null || ref.isBlank() ? null : ref.trim());
        b.setRefundedAt(java.time.Instant.now());
        mailer.send(b.getRenter().getEmail(), "Đã hoàn " + vnd(b.getRefundAmount()) + " cho đơn " + b.getCode(),
                "<p>Lentique đã chuyển <b>" + vnd(b.getRefundAmount()) + "</b> về tài khoản "
                        + b.getRefundBankAccount() + (b.getRefundBankName() == null ? "" : " (" + b.getRefundBankName() + ")")
                        + " cho đơn <b>" + b.getCode() + "</b>.</p>");
        return view(b);
    }

    private void notifyOwnerNewBooking(Booking b) {
        Product p = b.getProduct();
        mailer.send(p.getOwner().getEmail(), "Có người thuê " + p.getName(),
                "<p>Món <b>" + p.getName() + "</b> vừa được đặt thuê (" + b.getCode() + ").</p>"
                        + "<p>Ngày nhận: <b>" + b.getStartDate() + "</b> → trả: <b>" + b.getEndDate() + "</b> (" + b.getDays() + " ngày)<br>"
                        + "Tiền thuê: <b>" + vnd(b.getRentAmount()) + "</b> · Thanh toán: " + ("COD".equals(b.getPaymentMethod()) ? "COD" : "Đã chuyển khoản QR") + "</p>"
                        + "<p><a href=\"" + mailer.link("/account?tab=owner") + "\">Xác nhận đơn ngay</a></p>");
    }

    static String vnd(long amount) {
        return String.format(java.util.Locale.ROOT, "%,d₫", amount).replace(',', '.');
    }

    @Transactional
    public void review(UUID renterId, UUID bookingId, ReviewRequest req) {
        Booking b = find(bookingId);
        if (!b.getRenter().getId().equals(renterId)) throw new AppException(ErrorCode.FORBIDDEN);
        if (!Set.of("RETURNED", "COMPLETED", "DISPUTED").contains(b.getStatus()) || reviewRepository.existsByBookingId(bookingId))
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        Review r = new Review();
        r.setBookingId(bookingId);
        r.setProductId(b.getProduct().getId());
        r.setUserId(renterId);
        r.setRating(req.rating());
        r.setComment(req.comment());
        reviewRepository.save(r);
    }

    public OwnerStats ownerStats(UUID ownerId) {
        List<Booking> list = bookingRepository.findByProductOwnerIdOrderByCreatedAtDesc(ownerId);
        return new OwnerStats(list.size(),
                list.stream().filter(b -> "PENDING".equals(b.getStatus())).count(),
                list.stream().filter(b -> Set.of("CONFIRMED", "SHIPPING", "RENTED").contains(b.getStatus())).count(),
                list.stream().filter(b -> RENTED_OUT.contains(b.getStatus())).mapToLong(Booking::getRentAmount).sum(),
                productRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).size());
    }

    // ponytail: tính dòng tiền trong bộ nhớ trên toàn bộ booking; khi dữ liệu lớn → chuyển sang SUM query.
    public AdminStats adminStats() {
        List<Booking> all = bookingRepository.findAll();
        return new AdminStats(
                sum(all, "HELD", Booking::getDepositAmount),
                all.stream().filter(b -> "REFUNDED".equals(b.getDepositStatus())).mapToLong(b -> b.getDepositAmount() - b.getDeductionAmount()).sum(),
                all.stream().mapToLong(Booking::getDeductionAmount).sum(),
                all.stream().filter(b -> "PAID".equals(b.getPaymentStatus())).mapToLong(Booking::getRentAmount).sum(),
                all.stream().filter(b -> "UNPAID".equals(b.getPaymentStatus()) && !"CANCELLED".equals(b.getStatus())).mapToLong(Booking::getTotalAmount).sum(),
                productRepository.countByStatus(Product.PENDING),
                all.stream().filter(b -> "PROCESSING".equals(b.getRefundStatus())).count(),
                all.stream().filter(b -> "DISPUTED".equals(b.getStatus())).count(),
                all.size());
    }

    private static long sum(List<Booking> list, String depositStatus, java.util.function.ToLongFunction<Booking> f) {
        return list.stream().filter(b -> depositStatus.equals(b.getDepositStatus())).mapToLong(f).sum();
    }

    private Booking find(UUID id) {
        return bookingRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private CheckoutResponse toCheckout(List<Booking> list) {
        Booking first = list.get(0);
        boolean paid = list.stream().allMatch(b -> !"UNPAID".equals(b.getPaymentStatus()));
        return new CheckoutResponse(first.getCheckoutCode(), list.stream().mapToLong(Booking::getTotalAmount).sum(),
                first.getPaymentMethod(), paid ? "PAID" : "UNPAID", first.getPayosQrCode(), first.getPayosCheckoutUrl(),
                list.stream().map(this::view).toList());
    }

    private View view(Booking b) {
        Product p = b.getProduct();
        return new View(b.getId(), b.getCode(), b.getCheckoutCode(), p.getId(), p.getName(), p.cover(),
                b.getRenter().getName(), b.getStartDate(), b.getEndDate(), b.getDays(), b.getRentAmount(),
                b.getDepositAmount(), b.getShippingFee(), b.getTotalAmount(), b.getStatus(), b.getPaymentMethod(),
                b.getPaymentStatus(), b.getDepositStatus(), b.getDeductionAmount(), b.getAdminNote(),
                b.getRecipientName(), b.getPhone(), b.getAddress(), b.getDeliveryMethod(),
                b.getRefundBankAccount(), b.getRefundBankName(), b.getNote(), b.getRefundAmount(), b.getRefundedAt(),
                b.getRefundStatus(),
                reviewRepository.existsByBookingId(b.getId()), b.getCreatedAt());
    }
}
