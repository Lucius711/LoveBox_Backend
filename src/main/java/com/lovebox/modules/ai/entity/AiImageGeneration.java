package com.lovebox.modules.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_ai_image_generations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiImageGeneration {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "conversation_id") private UUID conversationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT") private String prompt;
    @Column(name = "revised_prompt", columnDefinition = "TEXT") private String revisedPrompt;
    @Column(name = "provider", nullable = false, length = 50) private String provider;
    @Column(name = "model", nullable = false, length = 100) private String model;
    @Column(name = "size", length = 20) private String size;
    @Column(name = "quality", length = 20) private String quality;
    @Column(name = "status", nullable = false, length = 32) private String status;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(name = "prompt_tokens") private Integer promptTokens;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
