package com.nexamarket.nexamarket.cart.infrastructure;

import com.nexamarket.nexamarket.cart.domain.Cart;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerIdAndStatus(UUID customerId, CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart left join fetch cart.items where cart.customerId = :customerId and cart.status = :status")
    Optional<Cart> findByCustomerIdAndStatusForUpdate(@Param("customerId") UUID customerId,
                                                       @Param("status") CartStatus status);
}
