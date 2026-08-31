package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.domain.SubOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface SubOrderRepository extends JpaRepository<SubOrder, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subOrder from SubOrder subOrder where subOrder.id = :id")
    Optional<SubOrder> findByIdForUpdate(@Param("id") UUID id);

    @Query("select subOrder from SubOrder subOrder join fetch subOrder.order where subOrder.sellerId = :sellerId")
    List<SubOrder> findBySellerIdWithOrder(@Param("sellerId") Long sellerId);

    @Query("select subOrder from SubOrder subOrder join fetch subOrder.order where subOrder.courierId = :courierId")
    List<SubOrder> findByCourierIdWithOrder(@Param("courierId") Long courierId);

    @Query("select subOrder from SubOrder subOrder join fetch subOrder.order")
    List<SubOrder> findAllWithOrder();

    @Query("select subOrder from SubOrder subOrder join fetch subOrder.order "
            + "where subOrder.createdAt >= :startsAt and subOrder.createdAt < :endsAt")
    List<SubOrder> findCreatedBetweenWithOrder(@Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);
}
