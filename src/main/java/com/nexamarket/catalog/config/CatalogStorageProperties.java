package com.nexamarket.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog.storage")
public record CatalogStorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        long maxUploadBytes
) {
}
