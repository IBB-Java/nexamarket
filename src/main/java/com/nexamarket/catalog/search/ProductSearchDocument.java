package com.nexamarket.catalog.search;

import com.nexamarket.catalog.entity.Category;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Document(indexName = "products", createIndex = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long sellerId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private List<Long> categoryIds;

    @Field(type = FieldType.Text)
    private List<String> categoryNames;

    @Field(type = FieldType.Double)
    private Double minPrice;

    @Field(type = FieldType.Double)
    private Double maxPrice;

    @Field(type = FieldType.Double)
    private Double sellerRating;

    @Field(type = FieldType.Long)
    private Long totalStock;

    @Field(type = FieldType.Boolean)
    private Boolean inStock;

    public static ProductSearchDocument from(Product product, BigDecimal sellerRating) {
        List<BigDecimal> variantPrices = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .toList();
        BigDecimal minPrice = variantPrices.stream().min(BigDecimal::compareTo).orElse(product.getBasePrice());
        BigDecimal maxPrice = variantPrices.stream().max(BigDecimal::compareTo).orElse(product.getBasePrice());
        long totalStock = product.getVariants().stream()
                .map(ProductVariant::getStockQuantity)
                .mapToLong(Integer::longValue)
                .sum();
        return ProductSearchDocument.builder()
                .id(product.getId().toString())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .description(product.getDescription())
                .status(product.getStatus().name())
                .categoryIds(product.getCategories().stream().map(Category::getId).sorted().toList())
                .categoryNames(product.getCategories().stream().map(Category::getName).sorted().toList())
                .minPrice(minPrice.doubleValue())
                .maxPrice(maxPrice.doubleValue())
                .sellerRating(sellerRating == null ? null : sellerRating.doubleValue())
                .totalStock(totalStock)
                .inStock(totalStock > 0)
                .build();
    }
}
