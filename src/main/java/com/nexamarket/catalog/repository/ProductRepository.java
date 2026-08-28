package com.nexamarket.catalog.repository;

import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"categories", "categories.parent", "variants", "variants.attributes"})
    List<Product> findAllBySellerIdAndStatusNotOrderByIdDesc(Long sellerId, ProductStatus status);

    @EntityGraph(attributePaths = {"categories", "categories.parent", "variants", "variants.attributes"})
    Optional<Product> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"categories", "categories.parent", "variants", "variants.attributes"})
    List<Product> findAllByOrderByIdAsc();
}
