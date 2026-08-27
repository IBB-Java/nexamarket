package com.nexamarket.catalog.repository;

import com.nexamarket.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ProductVariant variant
               set variant.stockQuantity = variant.stockQuantity - :quantity
             where variant.id = :variantId
               and variant.stockQuantity >= :quantity
            """)
    int decreaseStockIfAvailable(@Param("variantId") Long variantId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ProductVariant variant
               set variant.stockQuantity = variant.stockQuantity + :quantity
             where variant.id = :variantId
            """)
    int increaseStock(@Param("variantId") Long variantId, @Param("quantity") int quantity);
}
