package com.nexamarket.catalog.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Keeps the search view recoverable after a database migration, an index reset
 * or a local Docker volume restart. Normal updates are still handled by the
 * after-commit catalogue event listener.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ProductSearchBootstrapIndexer {

    private final ProductIndexingService productIndexingService;

    @Async("catalogIndexTaskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildSearchViewWhenApplicationIsReady() {
        int indexed = productIndexingService.indexAll();
        log.info("Search view is ready with {} catalogue products", indexed);
    }
}
