package com.lovebox.modules.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_payment_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "amount", nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "currency", nullable = false, length = 10) private String currency;
    @Column(name = "payment_method", nullable = false, length = 50) private String paymentMethod;
    @Column(name = "status", nullable = false, length = 32) private String status;
    @Column(name = "vietqr_payload", columnDefinition = "TEXT") private String vietqrPayload;
    @Column(name = "receipt_image_url", length = 1000) private String receiptImageUrl;
    @Column(name = "receipt_uploaded_at") private Instant receiptUploadedAt;
    @Column(name = "verified_at") private Instant verifiedAt;
    @Column(name = "verified_by", length = 255) private String verifiedBy;
    @Column(name = "rejection_reason", columnDefinition = "TEXT") private String rejectionReason;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
