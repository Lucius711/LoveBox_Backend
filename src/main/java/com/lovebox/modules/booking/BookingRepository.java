package com.lovebox.modules.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** Booking còn hiệu lực có khoảng khoá [start, end+1] giao với [from, to+1]. */
    @Query("""
            select count(b) > 0 from Booking b where b.product.id = :productId and b.status <> 'CANCELLED'
            and b.startDate <= :toPlusBuffer and b.endDate >= :fromMinusBuffer""")
    boolean existsConflict(UUID productId, LocalDate fromMinusBuffer, LocalDate toPlusBuffer);

    @Query("""
            select b from Booking b where b.product.id = :productId and b.status <> 'CANCELLED'
            and b.endDate >= :from order by b.startDate""")
    List<Booking> findActiveFrom(UUID productId, LocalDate from);

    /** Sản phẩm đang bị giữ hôm nay (đang cho thuê hoặc trong ngày đệm). */
    @Query("""
            select distinct b.product.id from Booking b where b.status <> 'CANCELLED'
            and b.startDate <= :today and b.endDate >= :yesterday""")
    List<UUID> findBusyProductIds(LocalDate today, LocalDate yesterday);

    List<Booking> findByRenterIdOrderByCreatedAtDesc(UUID renterId);

    List<Booking> findByProductOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Booking> findByCheckoutCode(long checkoutCode);

    List<Booking> findAllByOrderByCreatedAtDesc();

}
