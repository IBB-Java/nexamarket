package com.nexamarket.nexamarket.cart.infrastructure;

import com.nexamarket.nexamarket.cart.domain.CartItem;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CartItem item join fetch item.cart "
            + "where item.reservedUntil <= :expiredBefore and item.cart.status = :status")
    List<CartItem> findExpiredItemsForUpdate(@Param("expiredBefore") Instant expiredBefore,
                                             @Param("status") CartStatus status);
}
