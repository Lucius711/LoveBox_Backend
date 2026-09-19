package com.lovebox.modules.aichat.repository;

import com.lovebox.modules.aichat.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {
    List<ChatConversation> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<ChatConversation> findByIdAndUserId(UUID id, UUID userId);
}
