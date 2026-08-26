package com.nexamarket.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hangi ürüne ait olduğu (Product tablosu ile ilişki)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku; // Stok kodu (Stock Keeping Unit) - Örn: TSHIRT-KRMZ-M

    @Column(nullable = false)
    private String attributes; // Örn: "Renk: Kırmızı, Beden: M"

    @Column(nullable = false)
    private BigDecimal price; // Varyanta özel fiyat (Örn: XXL beden daha pahalı olabilir)

    @Column(nullable = false)
    private Integer stockQuantity; // Zorunlu stok takibi alanı
}
