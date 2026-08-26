package com.nexamarket.catalog.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductImageEventListener {

    private final ThumbnailProcessor thumbnailProcessor;

    @Async("thumbnailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImageUploaded(ProductImageUploadedEvent event) {
        thumbnailProcessor.process(event.imageId());
    }
}
