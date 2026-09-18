package com.lovebox.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_box_sizes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoxSize {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "code", nullable = false, unique = true, length = 50) private String code;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "price", nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "description", length = 500) private String description;
    @Column(name = "is_active", nullable = false) private Short isActive;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
