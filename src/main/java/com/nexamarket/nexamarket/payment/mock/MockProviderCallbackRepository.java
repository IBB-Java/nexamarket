package com.nexamarket.nexamarket.payment.mock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MockProviderCallbackRepository extends JpaRepository<MockProviderCallback, UUID> {

    boolean existsByProviderPaymentIdAndDeliveredAtIsNull(UUID providerPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select callback from MockProviderCallback callback "
            + "where callback.deliveredAt is null and callback.deliverAt <= :now")
    List<MockProviderCallback> findDueForDeliveryForUpdate(@Param("now") Instant now);
}
