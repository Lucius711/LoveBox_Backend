package com.lovebox.modules.aichat.repository;

import com.lovebox.modules.aichat.entity.ChatPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatPreferencesRepository extends JpaRepository<ChatPreferences, UUID> {
    Optional<ChatPreferences> findByUserId(UUID userId);
}
