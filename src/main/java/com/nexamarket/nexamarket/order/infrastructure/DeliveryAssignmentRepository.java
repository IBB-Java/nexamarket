package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.domain.DeliveryAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from DeliveryAssignment assignment "
            + "join fetch assignment.subOrder subOrder join fetch subOrder.order "
            + "where assignment.id = :id")
    Optional<DeliveryAssignment> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from DeliveryAssignment assignment "
            + "where assignment.subOrder.id = :subOrderId and assignment.activeAssignment = true")
    Optional<DeliveryAssignment> findActiveBySubOrderIdForUpdate(@Param("subOrderId") UUID subOrderId);

    @Query("select distinct assignment from DeliveryAssignment assignment "
            + "join fetch assignment.subOrder subOrder join fetch subOrder.order "
            + "left join fetch subOrder.items "
            + "where assignment.courierId = :courierId order by assignment.assignedAt desc")
    List<DeliveryAssignment> findByCourierIdWithOrder(@Param("courierId") Long courierId);

    @Query("select distinct assignment from DeliveryAssignment assignment "
            + "join fetch assignment.subOrder subOrder join fetch subOrder.order "
            + "left join fetch subOrder.items order by assignment.assignedAt desc")
    List<DeliveryAssignment> findAllWithOrder();

    @Query("select distinct assignment from DeliveryAssignment assignment "
            + "join fetch assignment.subOrder subOrder join fetch subOrder.order "
            + "left join fetch subOrder.items where subOrder.id = :subOrderId "
            + "order by assignment.assignedAt desc")
    List<DeliveryAssignment> findBySubOrderIdWithOrder(@Param("subOrderId") UUID subOrderId);

    boolean existsBySubOrder_IdAndCourierId(UUID subOrderId, Long courierId);
}
