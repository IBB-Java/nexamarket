package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    Optional<CustomerOrder> findBySourceCartId(UUID sourceCartId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct customerOrder from CustomerOrder customerOrder "
            + "left join fetch customerOrder.subOrders "
            + "where customerOrder.status = :status and customerOrder.createdAt <= :createdBefore")
    List<CustomerOrder> findByStatusAndCreatedAtBeforeForUpdate(@Param("status") OrderStatus status,
                                                                 @Param("createdBefore") Instant createdBefore);
}
