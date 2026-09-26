package com.lovebox.modules.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dtb_reviews")
@Getter @Setter
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "booking_id") private UUID bookingId;
    @Column(name = "product_id") private UUID productId;
    @Column(name = "user_id") private UUID userId;
    private int rating;
    private String comment;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
