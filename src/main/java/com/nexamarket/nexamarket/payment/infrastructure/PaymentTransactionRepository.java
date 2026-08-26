package com.nexamarket.nexamarket.payment.infrastructure;

import com.nexamarket.nexamarket.payment.domain.PaymentStatus;
import com.nexamarket.nexamarket.payment.domain.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentTransaction payment where payment.providerPaymentId = :providerPaymentId")
    Optional<PaymentTransaction> findByProviderPaymentIdForUpdate(@Param("providerPaymentId") UUID providerPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentTransaction payment "
            + "where payment.status = :status and payment.nextPollAt <= :now")
    List<PaymentTransaction> findDueForPollingForUpdate(@Param("status") PaymentStatus status,
                                                         @Param("now") Instant now);
}
