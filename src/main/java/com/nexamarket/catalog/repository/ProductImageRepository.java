package com.nexamarket.catalog.repository;

import com.nexamarket.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @EntityGraph(attributePaths = "product")
    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);

    /**
     * The most recently uploaded image is used as the product's cover in catalogue cards.
     * Fetching the product association here keeps the search response free from
     * one query per card.
     */
    @EntityGraph(attributePaths = "product")
    List<ProductImage> findAllByProduct_IdInOrderByProduct_IdAscIdDesc(Collection<Long> productIds);
}
