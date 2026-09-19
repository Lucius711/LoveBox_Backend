package com.lovebox.modules.aichat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_chat_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatPreferences {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(name = "preferred_language", length = 8) @Builder.Default private String preferredLanguage = "vi";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "style_preferences", columnDefinition = "jsonb") private String stylePreferences;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "occasion_interests", columnDefinition = "jsonb") private String occasionInterests;
    @Column(name = "budget_range", length = 50) private String budgetRange;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
