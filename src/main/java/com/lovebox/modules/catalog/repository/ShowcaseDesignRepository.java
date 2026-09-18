package com.lovebox.modules.catalog.repository;

import com.lovebox.modules.catalog.entity.ShowcaseDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShowcaseDesignRepository extends JpaRepository<ShowcaseDesign, UUID> {
    List<ShowcaseDesign> findByIsActiveOrderByDisplayOrderAsc(Short isActive);
    List<ShowcaseDesign> findByCategoryIdAndIsActiveOrderByDisplayOrderAsc(UUID categoryId, Short isActive);
}
