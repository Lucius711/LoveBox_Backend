package com.lovebox.modules.gift.repository;

import com.lovebox.modules.gift.entity.GiftDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GiftDesignRepository extends JpaRepository<GiftDesign, UUID> {
    List<GiftDesign> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
