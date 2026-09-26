package com.lovebox.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dtb_users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    public static final String RENTER = "RENTER", OWNER = "OWNER", ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "google_sub", unique = true, nullable = false)
    private String googleSub;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Builder.Default
    @Column(name = "role", nullable = false, length = 16)
    private String role = RENTER;

    private String phone;
    private String address;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "bank_name")
    private String bankName;

    @Builder.Default
    @Column(name = "trust_score", nullable = false)
    private int trustScore = 100;

    // ── Hồ sơ phong cách (onboarding) ──
    @Column(name = "height_cm") private Integer heightCm;
    @Column(name = "weight_kg") private Integer weightKg;
    private Integer bust;
    private Integer waist;
    private Integer hip;
    /** Size khách tự khai (S/M/L/XL) — ưu tiên hơn size tính từ chiều cao/cân nặng. */
    @Column(name = "clothing_size") private String clothingSize;
    @Column(name = "budget_max") private Long budgetMax;
    @Builder.Default @Convert(converter = StringListConverter.class)
    @Column(name = "fav_occasions", nullable = false) private List<String> favOccasions = List.of();
    @Builder.Default @Convert(converter = StringListConverter.class)
    @Column(name = "fav_styles", nullable = false) private List<String> favStyles = List.of();
    @Builder.Default @Convert(converter = StringListConverter.class)
    @Column(name = "fav_colors", nullable = false) private List<String> favColors = List.of();
    @Column(name = "fit_note") private String fitNote;
    @Column(name = "onboarded_at") private Instant onboardedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isOwner() { return OWNER.equals(role) || ADMIN.equals(role); }
}
