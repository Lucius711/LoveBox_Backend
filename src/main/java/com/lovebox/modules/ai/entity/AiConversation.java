package com.lovebox.modules.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_ai_conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiConversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "status", nullable = false, length = 32) private String status;
    @Column(name = "summary", columnDefinition = "TEXT") private String summary;
    @Column(name = "summarized_through_at") private Instant summarizedThroughAt;
    @Column(name = "total_prompt_tokens", nullable = false) private Long totalPromptTokens;
    @Column(name = "total_completion_tokens", nullable = false) private Long totalCompletionTokens;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
