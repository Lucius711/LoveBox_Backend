package com.lovebox.modules.order.repository;

import com.lovebox.modules.order.entity.ShippingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingInfoRepository extends JpaRepository<ShippingInfo, UUID> {
    Optional<ShippingInfo> findByOrderId(UUID orderId);
}
