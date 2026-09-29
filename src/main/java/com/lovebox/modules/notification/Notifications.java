package com.lovebox.modules.notification;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Thông báo trong tài khoản (chuông trên header). push() gọi cùng chỗ gửi email, chung transaction nghiệp vụ. */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class Notifications {

    private final NotificationRepository repo;

    public record Item(UUID id, String title, String link, boolean read, Instant createdAt) {}
    public record Inbox(long unread, List<Item> items) {}

    public void push(UUID userId, String title, String link) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title.length() > 255 ? title.substring(0, 252) + "..." : title);
        n.setLink(link);
        repo.save(n);
    }

    @GetMapping
    public ApiResponse<Inbox> inbox(@AuthenticationPrincipal UserPrincipal p) {
        List<Item> items = repo.findTop20ByUserIdOrderByCreatedAtDesc(p.getId()).stream()
                .map(n -> new Item(n.getId(), n.getTitle(), n.getLink(), n.getReadAt() != null, n.getCreatedAt())).toList();
        return ApiResponse.success(new Inbox(repo.countByUserIdAndReadAtIsNull(p.getId()), items));
    }

    @PostMapping("/read")
    @Transactional
    public ApiResponse<Void> readAll(@AuthenticationPrincipal UserPrincipal p) {
        repo.markAllRead(p.getId());
        return ApiResponse.success(null);
    }
}
