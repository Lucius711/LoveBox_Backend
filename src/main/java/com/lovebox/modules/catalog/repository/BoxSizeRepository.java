package com.lovebox.modules.catalog.repository;

import com.lovebox.modules.catalog.entity.BoxSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoxSizeRepository extends JpaRepository<BoxSize, UUID> {
    List<BoxSize> findByIsActiveOrderByPriceAsc(Short isActive);
}
