package com.lovebox.modules.ai.repository;

import com.lovebox.modules.ai.entity.AiGeneratedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiGeneratedImageRepository extends JpaRepository<AiGeneratedImage, UUID> {
    Optional<AiGeneratedImage> findByGenerationId(UUID generationId);
    List<AiGeneratedImage> findByGenerationIdIn(List<UUID> generationIds);
}
