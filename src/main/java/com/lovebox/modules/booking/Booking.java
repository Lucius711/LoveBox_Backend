package com.lovebox.modules.booking;

import com.lovebox.modules.product.Product;
import com.lovebox.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dtb_bookings")
@Getter @Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String code;
    @Column(name = "checkout_code") private long checkoutCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "renter_id")
    private User renter;

    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    private int days;
    @Column(name = "rent_amount") private long rentAmount;
    @Column(name = "deposit_amount") private long depositAmount;
    @Column(name = "shipping_fee") private long shippingFee;
    @Column(name = "total_amount") private long totalAmount;

    private String status = "PENDING";
    @Column(name = "payment_method") private String paymentMethod;
    @Column(name = "payment_status") private String paymentStatus = "UNPAID";
    @Column(name = "deposit_status") private String depositStatus = "PENDING";
    @Column(name = "deduction_amount") private long deductionAmount;
    @Column(name = "admin_note") private String adminNote;

    @Column(name = "recipient_name") private String recipientName;
    private String phone;
    private String address;
    @Column(name = "delivery_method") private String deliveryMethod;
    @Column(name = "refund_bank_account") private String refundBankAccount;
    @Column(name = "refund_bank_name") private String refundBankName;
    @Column(name = "refund_bank_bin") private String refundBankBin;
    private String note;
    @Column(name = "payos_checkout_url") private String payosCheckoutUrl;
    @Column(name = "payos_qr_code") private String payosQrCode;
    @Column(name = "refund_amount") private Long refundAmount;
    @Column(name = "refund_ref") private String refundRef;
    @Column(name = "refunded_at") private Instant refundedAt;
    @Column(name = "refund_status") private String refundStatus;
    @Column(name = "refund_attempts") private int refundAttempts;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
