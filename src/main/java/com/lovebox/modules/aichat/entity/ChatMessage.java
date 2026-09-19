package com.lovebox.modules.aichat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "conversation_id", nullable = false) private UUID conversationId;
    @Column(name = "role", nullable = false, length = 16) private String role;
    @Column(name = "content", nullable = false, columnDefinition = "TEXT") private String content;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "card_json", columnDefinition = "jsonb") private String cardJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "quick_replies", columnDefinition = "jsonb") private String quickReplies;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "actions_json", columnDefinition = "jsonb") private String actionsJson;
    @Column(name = "cached", nullable = false) @Builder.Default private Boolean cached = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
