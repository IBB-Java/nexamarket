package com.nexamarket.catalog.infrastructure;

import com.nexamarket.catalog.application.SellerDirectoryGateway;
import com.nexamarket.common.integration.IdentityUserSnapshot;
import com.nexamarket.common.integration.InternalRestClientFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RestSellerDirectoryGateway implements SellerDirectoryGateway {

    private final InternalRestClientFactory clients;

    public RestSellerDirectoryGateway(InternalRestClientFactory clients) {
        this.clients = clients;
    }

    @Override
    public Map<Long, String> displayNames(Set<Long> sellerIds) {
        if (sellerIds.isEmpty()) {
            return Map.of();
        }
        List<IdentityUserSnapshot> users = clients.create("identity-service.base-url")
                .get()
                .uri(builder -> builder.path("/internal/identity/users")
                        .queryParam("ids", sellerIds.toArray())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (users == null) {
            return Map.of();
        }
        return users.stream().collect(Collectors.toMap(IdentityUserSnapshot::id, IdentityUserSnapshot::displayName));
    }
}
