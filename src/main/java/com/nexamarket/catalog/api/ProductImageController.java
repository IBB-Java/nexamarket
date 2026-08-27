package com.nexamarket.catalog.api;

import com.nexamarket.catalog.application.ProductImageService;
import com.nexamarket.catalog.storage.StoredObject;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products/{productId}/images")
@RequiredArgsConstructor
@Validated
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> upload(
            @PathVariable @Positive Long productId,
            @RequestParam("file") MultipartFile file
    ) {
        ProductImageResponse response = productImageService.upload(productId, file);
        URI location = URI.create("/api/v1/products/" + productId + "/images/" + response.id());
        return ResponseEntity.accepted().location(location).body(response);
    }

    @GetMapping("/{imageId}")
    public ProductImageResponse getMetadata(
            @PathVariable @Positive Long productId,
            @PathVariable @Positive Long imageId
    ) {
        return productImageService.getMetadata(productId, imageId);
    }

    @GetMapping("/{imageId}/original")
    public ResponseEntity<ByteArrayResource> getOriginal(
            @PathVariable @Positive Long productId,
            @PathVariable @Positive Long imageId
    ) {
        return imageResponse(productImageService.getOriginal(productId, imageId), "original");
    }

    @GetMapping("/{imageId}/thumbnail")
    public ResponseEntity<ByteArrayResource> getThumbnail(
            @PathVariable @Positive Long productId,
            @PathVariable @Positive Long imageId
    ) {
        return imageResponse(productImageService.getThumbnail(productId, imageId), "thumbnail.jpg");
    }

    private ResponseEntity<ByteArrayResource> imageResponse(StoredObject object, String filename) {
        MediaType mediaType = MediaType.parseMediaType(object.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(object.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .body(new ByteArrayResource(object.content()));
    }
}
