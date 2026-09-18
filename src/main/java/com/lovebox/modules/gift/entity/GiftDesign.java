package com.lovebox.modules.gift.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_gift_designs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GiftDesign {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "source_type", nullable = false, length = 32) private String sourceType;
    @Column(name = "ai_generated_image_id") private UUID aiGeneratedImageId;
    @Column(name = "showcase_design_id") private UUID showcaseDesignId;
    @Column(name = "customization_data", columnDefinition = "TEXT") private String customizationData;
    @Column(name = "status", nullable = false, length = 32) private String status;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
