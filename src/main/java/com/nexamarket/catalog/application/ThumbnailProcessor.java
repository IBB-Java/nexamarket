package com.nexamarket.catalog.application;

import com.nexamarket.catalog.entity.ProductImage;
import com.nexamarket.catalog.entity.ProductImageStatus;
import com.nexamarket.catalog.repository.ProductImageRepository;
import com.nexamarket.catalog.storage.ObjectStorage;
import com.nexamarket.catalog.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailProcessor {

    private final ProductImageRepository productImageRepository;
    private final ObjectStorage objectStorage;
    private final ThumbnailGenerator thumbnailGenerator;

    @Transactional
    public void process(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new CatalogNotFoundException("Ürün görseli bulunamadı: " + imageId));
        try {
            StoredObject original = objectStorage.get(image.getOriginalObjectKey());
            byte[] thumbnail = thumbnailGenerator.generateJpeg(original.content());
            String thumbnailKey = image.getOriginalObjectKey().replaceFirst("/original\\.[^.]+$", "/thumbnail.jpg");
            objectStorage.put(thumbnailKey, thumbnail, "image/jpeg");
            image.setThumbnailObjectKey(thumbnailKey);
            image.setStatus(ProductImageStatus.READY);
            image.setFailureReason(null);
        } catch (RuntimeException exception) {
            image.setStatus(ProductImageStatus.FAILED);
            image.setFailureReason(limitMessage(exception.getMessage()));
            log.warn("Thumbnail generation failed for image {}", imageId, exception);
        }
    }

    private String limitMessage(String message) {
        if (message == null) {
            return "Bilinmeyen thumbnail hatası";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
