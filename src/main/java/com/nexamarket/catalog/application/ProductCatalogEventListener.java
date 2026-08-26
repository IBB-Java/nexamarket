package com.nexamarket.catalog.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogEventListener {

    private final ProductIndexingService productIndexingService;

    @Async("catalogIndexTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(ProductCatalogChangedEvent event) {
        try {
            productIndexingService.index(event.productId());
        } catch (RuntimeException exception) {
            log.warn("Product {} could not be indexed", event.productId(), exception);
        }
    }
}
