package com.nexamarket.nexamarket.cart.infrastructure;

import com.nexamarket.nexamarket.cart.application.CheckoutOrderRequest;
import com.nexamarket.nexamarket.cart.application.OrderCreation;
import com.nexamarket.nexamarket.cart.application.OrderCreationGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderCreationClient implements OrderCreationGateway {

    private final RestClient restClient;

    public OrderCreationClient(RestClient.Builder restClientBuilder,
                               @Value("${order-service.base-url}") String orderServiceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(orderServiceBaseUrl).build();
    }

    @Override
    public OrderCreation createOrder(CheckoutOrderRequest request) {
        return restClient.post()
                .uri("/internal/orders/from-cart")
                .body(request)
                .retrieve()
                .body(OrderCreation.class);
    }
}
