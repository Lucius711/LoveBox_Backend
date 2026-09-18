package com.lovebox.modules.notification.serviceimpl;

import com.lovebox.modules.notification.entity.AdminNotification;
import com.lovebox.modules.notification.repository.AdminNotificationRepository;
import com.lovebox.modules.notification.service.NotificationService;
import com.lovebox.modules.order.entity.Order;
import com.lovebox.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final AdminNotificationRepository notificationRepository;
    private final OrderRepository orderRepository;

    @Value("${app.notification.resend-api-key}") private String resendApiKey;
    @Value("${app.notification.from-email}")      private String fromEmail;
    @Value("${app.notification.admin-email}")     private String adminEmail;

    private static final String RESEND_URL = "https://api.resend.com/emails";

    @Override
    @Async
    public void notifyNewOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        String subject = "🎁 Đơn hàng mới #" + order.getOrderCode();
        String html = """
                <h2>Đơn hàng mới</h2>
                <table>
                  <tr><td><b>Mã đơn</b></td><td>%s</td></tr>
                  <tr><td><b>Tổng tiền</b></td><td>%s VNĐ</td></tr>
                  <tr><td><b>Trạng thái</b></td><td>%s</td></tr>
                </table>
                """.formatted(order.getOrderCode(), order.getTotalAmount(), order.getStatus());

        sendEmail(orderId, "NEW_ORDER", subject, html);
    }

    @Override
    @Async
    public void notifyReceiptUploaded(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        String subject = "📋 Biên lai chờ xác minh #" + order.getOrderCode();
        String html = """
                <h2>Biên lai đã được tải lên</h2>
                <p>Mã đơn: <b>%s</b></p>
                <p>Vui lòng đăng nhập admin để xác minh thanh toán.</p>
                """.formatted(order.getOrderCode());

        sendEmail(orderId, "RECEIPT_UPLOADED", subject, html);
    }

    // ------------------------------------------------------------------ //

    private void sendEmail(UUID orderId, String type, String subject, String html) {
        AdminNotification notification = notificationRepository.save(
                AdminNotification.builder()
                        .orderId(orderId).channel("EMAIL")
                        .type(type).message(subject).status("PENDING")
                        .build());

        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Resend API key not configured, skipping notification");
            return;
        }

        try {
            RestClient.create().post()
                    .uri(RESEND_URL)
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "from",    fromEmail,
                            "to",      List.of(adminEmail),
                            "subject", subject,
                            "html",    html
                    ))
                    .retrieve()
                    .toBodilessEntity();

            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
            log.info("[Resend] email sent — type={} orderId={}", type, orderId);

        } catch (Exception e) {
            log.error("[Resend] failed to send email — type={}", type, e);
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
        }
        notificationRepository.save(notification);
    }
}
