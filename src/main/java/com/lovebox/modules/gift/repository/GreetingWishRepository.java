package com.lovebox.modules.gift.repository;

import com.lovebox.modules.gift.entity.GreetingWish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GreetingWishRepository extends JpaRepository<GreetingWish, UUID> {
    List<GreetingWish> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<GreetingWish> findByQrToken(UUID qrToken);
}
