package com.nexamarket.nexamarket.order.api;

import com.nexamarket.common.integration.OrderReportRowSnapshot;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import com.nexamarket.nexamarket.order.infrastructure.SubOrderRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/internal/orders/report-data")
public class OrderReportInternalController {

    private final SubOrderRepository subOrderRepository;

    public OrderReportInternalController(SubOrderRepository subOrderRepository) {
        this.subOrderRepository = subOrderRepository;
    }

    @GetMapping("/seller")
    public List<OrderReportRowSnapshot> sellerRows(@RequestParam Long sellerId) {
        return subOrderRepository.findBySellerIdWithOrder(sellerId).stream().map(this::snapshot).toList();
    }

    @GetMapping("/daily")
    public List<OrderReportRowSnapshot> dailyRows(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startsAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endsAt) {
        return subOrderRepository.findCreatedBetweenWithOrder(startsAt, endsAt).stream().map(this::snapshot).toList();
    }

    private OrderReportRowSnapshot snapshot(SubOrder row) {
        return new OrderReportRowSnapshot(row.getId(), row.getSellerId(), row.getStatus().name(),
                row.getSubtotal(), row.getCreatedAt());
    }
}
