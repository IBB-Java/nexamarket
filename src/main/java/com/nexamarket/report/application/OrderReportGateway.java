package com.nexamarket.report.application;

import com.nexamarket.common.integration.OrderReportRowSnapshot;

import java.time.Instant;
import java.util.List;

public interface OrderReportGateway {

    List<OrderReportRowSnapshot> sellerRows(Long sellerId);

    List<OrderReportRowSnapshot> dailyRows(Instant startsAt, Instant endsAt);
}
