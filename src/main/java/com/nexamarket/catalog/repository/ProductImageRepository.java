package com.nexamarket.catalog.repository;

import com.nexamarket.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @EntityGraph(attributePaths = "product")
    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);
}
