package com.nexamarket.catalog.repository;

import com.nexamarket.catalog.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllBySellerId(Long sellerId);

    @EntityGraph(attributePaths = {"categories", "categories.parent", "variants", "variants.attributes"})
    Optional<Product> findDetailedById(Long id);
}
