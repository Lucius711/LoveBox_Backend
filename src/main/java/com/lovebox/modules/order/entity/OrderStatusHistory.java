package com.lovebox.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_order_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "from_status", length = 32) private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 32) private String toStatus;
    @Column(name = "note", columnDefinition = "TEXT") private String note;
    @Column(name = "changed_by", length = 255) private String changedBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
