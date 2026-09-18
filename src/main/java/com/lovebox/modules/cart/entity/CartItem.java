package com.lovebox.modules.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_cart_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "cart_id", nullable = false) private UUID cartId;
    @Column(name = "gift_design_id", nullable = false) private UUID giftDesignId;
    @Column(name = "greeting_wish_id") private UUID greetingWishId;
    @Column(name = "box_size_id", nullable = false) private UUID boxSizeId;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
