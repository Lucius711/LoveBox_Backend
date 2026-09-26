package com.lovebox.modules.stylist;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Một cuộc chat với trợ lý AI. messages giữ đúng dạng FE hiển thị: {from:'user',text} | {from:'bot',result}. */
@Entity
@Table(name = "dtb_chat_sessions")
@Getter @Setter
public class ChatSession {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id") private UUID userId;
    private String title;
    /** Các câu khách đã gõ, nối lại → AI hiểu yêu cầu tinh chỉnh dần ("màu đen thôi"). */
    private String prompt = "";
    @JdbcTypeCode(SqlTypes.JSON) private List<Map<String, Object>> messages = new ArrayList<>();
    private boolean archived;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
