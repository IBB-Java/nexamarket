package com.nexamarket.catalog.application;

import com.nexamarket.catalog.config.CatalogIndexingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogEventListener {

    private final ProductIndexingService productIndexingService;
    private final CatalogIndexingProperties indexingProperties;

    @Async("catalogIndexTaskExecutor")
    @CacheEvict(cacheNames = "catalogSearch", allEntries = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(ProductCatalogChangedEvent event) {
        Instant deadline = event.occurredAt().plus(indexingProperties.getMaxDelay());
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= indexingProperties.getMaxAttempts(); attempt++) {
            if (Instant.now().isAfter(deadline)) {
                break;
            }
            try {
                productIndexingService.index(event.productId());
                log.debug("Product {} indexed on attempt {}", event.productId(), attempt);
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn("Product {} could not be indexed on attempt {}", event.productId(), attempt, exception);
                if (attempt < indexingProperties.getMaxAttempts() && !pauseBeforeRetry(deadline)) {
                    break;
                }
            }
        }

        log.error(
                "Product {} was not indexed within the configured consistency bound of {}",
                event.productId(),
                indexingProperties.getMaxDelay(),
                lastFailure);
    }

    private boolean pauseBeforeRetry(Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isZero() || remaining.isNegative()) {
            return false;
        }
        Duration pause = indexingProperties.getRetryDelay().compareTo(remaining) < 0
                ? indexingProperties.getRetryDelay()
                : remaining;
        try {
            Thread.sleep(pause.toMillis());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
