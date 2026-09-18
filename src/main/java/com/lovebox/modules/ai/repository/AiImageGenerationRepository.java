package com.lovebox.modules.ai.repository;

import com.lovebox.modules.ai.entity.AiImageGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiImageGenerationRepository extends JpaRepository<AiImageGeneration, UUID> {
    List<AiImageGeneration> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
}
