package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.domain.ReturnRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select returnRequest from ReturnRequest returnRequest join fetch returnRequest.subOrder "
            + "where returnRequest.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") UUID id);
}
