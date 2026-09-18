package com.lovebox.modules.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_showcase_designs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShowcaseDesign {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "category_id") private UUID categoryId;
    @Column(name = "name", nullable = false, length = 255) private String name;
    @Column(name = "thumbnail_url", nullable = false, length = 1000) private String thumbnailUrl;
    @Column(name = "description", length = 500) private String description;
    @Column(name = "is_active", nullable = false) private Short isActive;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
