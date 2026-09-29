package com.lovebox.modules.owner;

import com.lovebox.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Đơn đăng ký làm Chủ đồ. Admin duyệt → user.role = OWNER. */
@Entity
@Table(name = "dtb_owner_applications")
@Getter @Setter
public class OwnerApplication {
    public static final String PENDING = "PENDING", APPROVED = "APPROVED", REJECTED = "REJECTED";

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    private String phone;
    private String address;
    @Column(name = "bank_account") private String bankAccount;
    @Column(name = "bank_name") private String bankName;
    private String intro;
    private String status = PENDING;
    @Column(name = "reject_reason") private String rejectReason;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "reviewed_at") private Instant reviewedAt;
}
