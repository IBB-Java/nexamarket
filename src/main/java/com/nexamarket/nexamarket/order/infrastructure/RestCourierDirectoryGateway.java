package com.nexamarket.nexamarket.order.infrastructure;

import com.nexamarket.common.integration.IdentityUserSnapshot;
import com.nexamarket.common.integration.InternalRestClientFactory;
import com.nexamarket.nexamarket.order.application.CourierDirectoryGateway;
import org.springframework.stereotype.Component;

@Component
public class RestCourierDirectoryGateway implements CourierDirectoryGateway {

    private final InternalRestClientFactory clients;

    public RestCourierDirectoryGateway(InternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public boolean isActiveCourier(Long userId) {
        IdentityUserSnapshot user = clients.create("identity-service.base-url")
                .get()
                .uri("/internal/identity/users/{userId}", userId)
                .retrieve()
                .body(IdentityUserSnapshot.class);
        return user != null && "COURIER".equals(user.role()) && "ACTIVE".equals(user.status());
    }
}
