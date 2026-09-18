package com.lovebox.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_shipping_info")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingInfo {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "order_id", nullable = false, unique = true) private UUID orderId;
    @Column(name = "recipient_name", nullable = false, length = 255) private String recipientName;
    @Column(name = "phone", nullable = false, length = 20) private String phone;
    @Column(name = "address_line1", nullable = false, length = 500) private String addressLine1;
    @Column(name = "address_line2", length = 500) private String addressLine2;
    @Column(name = "city", nullable = false, length = 100) private String city;
    @Column(name = "district", length = 100) private String district;
    @Column(name = "ward", length = 100) private String ward;
    @Column(name = "delivery_note", length = 500) private String deliveryNote;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
