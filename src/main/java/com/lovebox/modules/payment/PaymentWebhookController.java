package com.lovebox.modules.payment;

import com.lovebox.modules.booking.BookingService;
import com.lovebox.modules.payment.payos.PayOSClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PayOSClient payOSClient;
    private final BookingService bookingService;

    /** PayOS gọi khi khách quét QR chuyển khoản thành công (public, xác thực bằng chữ ký). */
    @PostMapping("/payment/payos-webhook")
    public ResponseEntity<Map<String, String>> payosWebhook(@RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data == null || !payOSClient.verifyWebhook(data, String.valueOf(payload.get("signature")))) {
                log.warn("PayOS webhook: invalid signature");
                return ResponseEntity.badRequest().body(Map.of("error", "invalid signature"));
            }
            if ("00".equals(String.valueOf(data.get("code"))))
                bookingService.markPaid(Long.parseLong(String.valueOf(data.get("orderCode"))),
                        (String) data.get("counterAccountBankId"), (String) data.get("counterAccountNumber"),
                        (String) data.get("counterAccountBankName"));
        } catch (Exception e) {
            log.error("PayOS webhook error", e);
        }
        return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));  // luôn 200 cho PayOS
    }
}
