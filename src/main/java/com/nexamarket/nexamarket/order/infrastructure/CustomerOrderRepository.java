package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Optional<CustomerOrder> findBySourceCartId(UUID sourceCartId);
}
