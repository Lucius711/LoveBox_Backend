package com.lovebox.modules.auth.repository;

import com.lovebox.modules.auth.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    Optional<AuthSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    @Modifying
    @Query("UPDATE AuthSession s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
    void revokeAllByUserId(UUID userId, Instant now);
}
