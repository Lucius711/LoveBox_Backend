package com.lovebox.modules.payment.dto.response;

import com.lovebox.modules.payment.entity.PaymentTransaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String vietqrPayload;
    private String receiptImageUrl;
    private Instant receiptUploadedAt;
    private Instant verifiedAt;
    private Instant createdAt;

    public static PaymentResponse from(PaymentTransaction t) {
        return PaymentResponse.builder()
                .id(t.getId()).orderId(t.getOrderId()).amount(t.getAmount())
                .currency(t.getCurrency()).paymentMethod(t.getPaymentMethod())
                .status(t.getStatus()).vietqrPayload(t.getVietqrPayload())
                .receiptImageUrl(t.getReceiptImageUrl())
                .receiptUploadedAt(t.getReceiptUploadedAt())
                .verifiedAt(t.getVerifiedAt()).createdAt(t.getCreatedAt()).build();
    }
}
