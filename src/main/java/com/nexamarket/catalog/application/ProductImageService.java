package com.nexamarket.catalog.application;

import com.nexamarket.catalog.api.ProductImageResponse;
import com.nexamarket.catalog.config.CatalogStorageProperties;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductImage;
import com.nexamarket.catalog.entity.ProductImageStatus;
import com.nexamarket.catalog.repository.ProductImageRepository;
import com.nexamarket.catalog.repository.ProductRepository;
import com.nexamarket.catalog.storage.ObjectStorage;
import com.nexamarket.catalog.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(MediaTypes.JPEG, MediaTypes.PNG);

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ObjectStorage objectStorage;
    private final CatalogStorageProperties storageProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductImageResponse upload(Long productId, MultipartFile file) {
        validate(file);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CatalogNotFoundException("Ürün bulunamadı: " + productId));

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String extension = contentType.equals(MediaTypes.PNG) ? "png" : "jpg";
        String imagePath = "products/" + productId + "/images/" + UUID.randomUUID();
        String originalKey = imagePath + "/original." + extension;

        byte[] content = readContent(file);
        objectStorage.put(originalKey, content, contentType);

        ProductImage image = productImageRepository.save(ProductImage.builder()
                .product(product)
                .originalObjectKey(originalKey)
                .originalContentType(contentType)
                .status(ProductImageStatus.PENDING_THUMBNAIL)
                .build());
        eventPublisher.publishEvent(new ProductImageUploadedEvent(image.getId()));
        return ProductImageResponse.from(image);
    }

    @Transactional(readOnly = true)
    public ProductImageResponse getMetadata(Long productId, Long imageId) {
        return ProductImageResponse.from(findImage(productId, imageId));
    }

    @Transactional(readOnly = true)
    public StoredObject getOriginal(Long productId, Long imageId) {
        ProductImage image = findImage(productId, imageId);
        return objectStorage.get(image.getOriginalObjectKey());
    }

    @Transactional(readOnly = true)
    public StoredObject getThumbnail(Long productId, Long imageId) {
        ProductImage image = findImage(productId, imageId);
        if (image.getStatus() != ProductImageStatus.READY || image.getThumbnailObjectKey() == null) {
            throw new ThumbnailNotReadyException("Thumbnail henüz hazır değil: " + imageId);
        }
        return objectStorage.get(image.getThumbnailObjectKey());
    }

    private ProductImage findImage(Long productId, Long imageId) {
        return productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new CatalogNotFoundException("Ürün görseli bulunamadı: " + imageId));
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidProductImageException("Görsel dosyası boş olamaz.");
        }
        if (file.getSize() > storageProperties.maxUploadBytes()) {
            throw new InvalidProductImageException("Görsel en fazla " + storageProperties.maxUploadBytes() + " bayt olabilir.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidProductImageException("Yalnızca JPEG ve PNG görseller desteklenir.");
        }
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new InvalidProductImageException("Görsel dosyası okunamadı.", exception);
        }
    }

    static final class MediaTypes {
        static final String JPEG = "image/jpeg";
        static final String PNG = "image/png";

        private MediaTypes() {
        }
    }
}
