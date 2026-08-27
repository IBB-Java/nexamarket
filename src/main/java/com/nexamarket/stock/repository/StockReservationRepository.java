package com.nexamarket.stock.repository;

import com.nexamarket.stock.entity.StockReservation;
import com.nexamarket.stock.entity.StockReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation from StockReservation reservation
            join fetch reservation.variant
            where reservation.reservationCode = :reservationCode
            """)
    Optional<StockReservation> findLockedByReservationCode(@Param("reservationCode") String reservationCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation from StockReservation reservation
            join fetch reservation.variant
            where reservation.status = :status
              and reservation.expiresAt <= :expiresAt
            """)
    List<StockReservation> findExpiredReservationsForUpdate(
            @Param("status") StockReservationStatus status,
            @Param("expiresAt") LocalDateTime expiresAt);
}
