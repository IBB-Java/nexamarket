package com.nexamarket.catalog.application;

import com.nexamarket.catalog.config.CatalogIndexingProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class ProductCatalogEventListenerTest {

    @Test
    void retriesTransientIndexingFailureWithinConsistencyBound() {
        ProductIndexingService indexingService = mock(ProductIndexingService.class);
        CatalogIndexingProperties properties = properties(3);
        ProductCatalogEventListener listener = new ProductCatalogEventListener(indexingService, properties);
        doThrow(new IllegalStateException("Elasticsearch unavailable"))
                .doThrow(new IllegalStateException("Elasticsearch unavailable"))
                .doNothing()
                .when(indexingService).index(41L);

        listener.onProductChanged(ProductCatalogChangedEvent.now(41L));

        verify(indexingService, times(3)).index(41L);
    }

    @Test
    void stopsAfterConfiguredAttemptLimit() {
        ProductIndexingService indexingService = mock(ProductIndexingService.class);
        CatalogIndexingProperties properties = properties(2);
        ProductCatalogEventListener listener = new ProductCatalogEventListener(indexingService, properties);
        doThrow(new IllegalStateException("Elasticsearch unavailable"))
                .when(indexingService).index(42L);

        listener.onProductChanged(ProductCatalogChangedEvent.now(42L));

        verify(indexingService, times(2)).index(42L);
    }

    private CatalogIndexingProperties properties(int maxAttempts) {
        CatalogIndexingProperties properties = new CatalogIndexingProperties();
        properties.setMaxDelay(Duration.ofSeconds(1));
        properties.setRetryDelay(Duration.ofMillis(1));
        properties.setMaxAttempts(maxAttempts);
        return properties;
    }
}
