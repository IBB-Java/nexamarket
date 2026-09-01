package com.nexamarket.common.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InternalRestClientFactory {

    private final RestClient.Builder builder;
    private final Environment environment;
    private final String apiKey;

    public InternalRestClientFactory(RestClient.Builder builder, Environment environment,
                                     @Value("${internal.api-key}") String apiKey) {
        this.builder = builder;
        this.environment = environment;
        this.apiKey = apiKey;
    }

    /** Resolves the URL at call time so random-port integration tests can use the real HTTP boundary. */
    public RestClient create(String baseUrlProperty) {
        String baseUrl = environment.getRequiredProperty(baseUrlProperty);
        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, apiKey)
                .build();
    }
}
