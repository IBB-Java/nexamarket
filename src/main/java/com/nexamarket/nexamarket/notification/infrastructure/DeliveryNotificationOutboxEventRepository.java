package com.nexamarket.nexamarket.notification.infrastructure;

import com.nexamarket.nexamarket.notification.domain.DeliveryNotificationOutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeliveryNotificationOutboxEventRepository
        extends JpaRepository<DeliveryNotificationOutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from DeliveryNotificationOutboxEvent event "
            + "where event.publishedAt is null and event.nextAttemptAt <= :now")
    List<DeliveryNotificationOutboxEvent> findDueForPublishForUpdate(@Param("now") Instant now);
}
