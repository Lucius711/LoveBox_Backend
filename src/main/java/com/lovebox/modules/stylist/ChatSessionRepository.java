package com.lovebox.modules.stylist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByUserIdAndArchivedOrderByUpdatedAtDesc(UUID userId, boolean archived, Pageable pageable);
    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);
}
