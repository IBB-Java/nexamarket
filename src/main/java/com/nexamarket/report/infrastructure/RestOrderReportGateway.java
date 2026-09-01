package com.nexamarket.report.infrastructure;

import com.nexamarket.common.integration.InternalRestClientFactory;
import com.nexamarket.common.integration.OrderReportRowSnapshot;
import com.nexamarket.report.application.OrderReportGateway;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RestOrderReportGateway implements OrderReportGateway {

    private final InternalRestClientFactory clients;

    public RestOrderReportGateway(InternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public List<OrderReportRowSnapshot> sellerRows(Long sellerId) {
        return get(builder -> builder.path("/internal/orders/report-data/seller")
                .queryParam("sellerId", sellerId).build());
    }

    @Override
    public List<OrderReportRowSnapshot> dailyRows(Instant startsAt, Instant endsAt) {
        return get(builder -> builder.path("/internal/orders/report-data/daily")
                .queryParam("startsAt", startsAt)
                .queryParam("endsAt", endsAt)
                .build());
    }

    private List<OrderReportRowSnapshot> get(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
        List<OrderReportRowSnapshot> rows = clients.create("order-service.base-url")
                .get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return rows == null ? List.of() : rows;
    }
}
