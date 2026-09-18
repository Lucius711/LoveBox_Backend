package com.lovebox.modules.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_ai_generated_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiGeneratedImage {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "generation_id", nullable = false) private UUID generationId;
    @Column(name = "image_url", nullable = false, length = 1000) private String imageUrl;
    @Column(name = "storage_path", length = 500) private String storagePath;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
