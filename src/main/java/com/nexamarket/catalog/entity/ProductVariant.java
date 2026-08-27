package com.nexamarket.catalog.entity;

import com.nexamarket.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    @ElementCollection
    @CollectionTable(name = "product_variant_attributes", joinColumns = @JoinColumn(name = "variant_id"))
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value", nullable = false)
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Version
    private Long version;
}
