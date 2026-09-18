package com.lovebox.modules.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "dtb_admin_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminNotification {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "order_id")                                   private UUID    orderId;
    @Column(name = "type",    nullable = false, length = 64)     private String  type;
    @Column(name = "channel", nullable = false, length = 32)     private String  channel;
    @Column(name = "message", nullable = false, columnDefinition = "TEXT") private String message;
    @Column(name = "status",  nullable = false, length = 32)     private String  status;
    @Column(name = "error_message", columnDefinition = "TEXT")   private String  errorMessage;
    @Column(name = "sent_at")                                    private Instant sentAt;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)              private Instant createdAt;
}
