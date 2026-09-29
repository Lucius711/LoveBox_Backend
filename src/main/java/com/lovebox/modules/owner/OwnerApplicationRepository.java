package com.lovebox.modules.owner;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnerApplicationRepository extends JpaRepository<OwnerApplication, UUID> {
    Optional<OwnerApplication> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserIdAndStatus(UUID userId, String status);
    @EntityGraph(attributePaths = "user")
    List<OwnerApplication> findByStatusOrderByCreatedAtAsc(String status);
    @EntityGraph(attributePaths = "user")
    List<OwnerApplication> findAllByOrderByCreatedAtDesc();
}
