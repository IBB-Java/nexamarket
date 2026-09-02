package com.nexamarket.nexamarket.order.application;

import com.nexamarket.nexamarket.order.domain.CustomerOrder;
import com.nexamarket.nexamarket.order.domain.OrderStatus;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Assigns paid deliveries to the least busy active courier. */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutomaticCourierAssignmentService {

    private static final EnumSet<OrderStatus> OPEN_DELIVERY_STATUSES = EnumSet.of(
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.RETURN_REQUESTED);

    private final CourierDirectoryGateway courierDirectoryGateway;
    private final SubOrderRepository subOrderRepository;

    public void assignAfterPayment(CustomerOrder order) {
        List<Long> courierIds = courierDirectoryGateway.findActiveCourierIds().stream()
                .sorted()
                .toList();
        if (courierIds.isEmpty()) {
            log.warn("No active courier exists; paid order {} remains unassigned", order.getId());
            return;
        }

        Map<Long, Long> activeLoads = new HashMap<>();
        courierIds.forEach(id -> activeLoads.put(id,
                subOrderRepository.countByCourierIdAndStatusIn(id, OPEN_DELIVERY_STATUSES)));

        for (SubOrder subOrder : order.getSubOrders()) {
            if (subOrder.getCourierId() != null) {
                continue;
            }
            Long selectedCourierId = courierIds.stream()
                    .min(Comparator.<Long>comparingLong(id -> activeLoads.get(id))
                            .thenComparingLong(Long::longValue))
                    .orElseThrow();
            subOrder.assignCourier(selectedCourierId);
            activeLoads.compute(selectedCourierId, (id, load) -> load + 1);
            log.info("Sub-order {} automatically assigned to courier {}", subOrder.getId(), selectedCourierId);
        }
    }
}
