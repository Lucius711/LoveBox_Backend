package com.lovebox.modules.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    List<Product> findByStatusOrderByCreatedAtDesc(String status);

    List<Product> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    long countByStatus(String status);

    /** Số sản phẩm còn dùng ảnh này — chỉ xoá file trên R2 khi không còn ai dùng. */
    @Query(value = "select count(*) from dtb_product_images where url = :url", nativeQuery = true)
    long countImageRefs(String url);

    /** Khoá dòng sản phẩm khi đặt thuê → 2 người đặt cùng lúc được xử lý tuần tự. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(UUID id);
}
