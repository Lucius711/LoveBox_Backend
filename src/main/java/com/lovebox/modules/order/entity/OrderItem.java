package com.lovebox.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "gift_design_id", nullable = false) private UUID giftDesignId;
    @Column(name = "greeting_wish_id") private UUID greetingWishId;
    @Column(name = "box_size_id", nullable = false) private UUID boxSizeId;
    @Column(name = "box_size_code", nullable = false, length = 50) private String boxSizeCode;
    @Column(name = "box_size_name", nullable = false, length = 100) private String boxSizeName;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2) private BigDecimal lineTotal;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
