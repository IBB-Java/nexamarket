package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.domain.ReturnRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select returnRequest from ReturnRequest returnRequest join fetch returnRequest.subOrder "
            + "where returnRequest.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") UUID id);

    @Query("select returnRequest from ReturnRequest returnRequest "
            + "join fetch returnRequest.subOrder subOrder join fetch subOrder.order customerOrder "
            + "where customerOrder.customerId = :customerId order by returnRequest.createdAt desc")
    List<ReturnRequest> findForCustomer(@Param("customerId") Long customerId);

    @Query("select returnRequest from ReturnRequest returnRequest "
            + "join fetch returnRequest.subOrder subOrder join fetch subOrder.order "
            + "where subOrder.sellerId = :sellerId order by returnRequest.createdAt desc")
    List<ReturnRequest> findForSeller(@Param("sellerId") Long sellerId);

    @Query("select returnRequest from ReturnRequest returnRequest "
            + "join fetch returnRequest.subOrder subOrder join fetch subOrder.order "
            + "order by returnRequest.createdAt desc")
    List<ReturnRequest> findAllWithOrderDetails();
}
