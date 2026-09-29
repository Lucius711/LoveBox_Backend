package com.lovebox.modules.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dtb_notifications")
@Getter @Setter
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id") private UUID userId;
    private String title;
    private String link;
    @Column(name = "read_at") private Instant readAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
