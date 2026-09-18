package com.lovebox.modules.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_ai_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "conversation_id", nullable = false) private UUID conversationId;
    @Column(name = "role", nullable = false, length = 16) private String role;
    @Column(name = "content", nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "prompt_tokens") private Integer promptTokens;
    @Column(name = "completion_tokens") private Integer completionTokens;
    @Column(name = "processing_time_ms") private Integer processingTimeMs;
    @Column(name = "from_cache", nullable = false) private Short fromCache;
    @Column(name = "model", length = 100) private String model;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
