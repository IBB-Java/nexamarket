package com.nexamarket.nexamarket.notification.infrastructure;

import com.nexamarket.nexamarket.notification.domain.NotificationMessage;
import com.nexamarket.nexamarket.notification.domain.NotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, UUID> {
    boolean existsByDeduplicationKey(String deduplicationKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select message from NotificationMessage message where message.status in :statuses and message.nextAttemptAt <= :now")
    List<NotificationMessage> findDueForDeliveryForUpdate(@Param("statuses") Collection<NotificationStatus> statuses, @Param("now") Instant now);
}
