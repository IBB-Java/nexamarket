package com.nexamarket.catalog.application;

import com.nexamarket.catalog.api.CategoryResponse;
import com.nexamarket.catalog.api.CreateCategoryRequest;
import com.nexamarket.catalog.api.CreateProductRequest;
import com.nexamarket.catalog.api.CreateProductVariantRequest;
import com.nexamarket.catalog.api.ProductResponse;
import com.nexamarket.catalog.api.UpdateProductRequest;
import com.nexamarket.catalog.api.UpdateProductVariantRequest;
import com.nexamarket.catalog.entity.Category;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.entity.ProductVariant;
import com.nexamarket.catalog.repository.CategoryRepository;
import com.nexamarket.catalog.repository.ProductRepository;
import com.nexamarket.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CatalogConflictException("Bu isimde bir kategori zaten mevcut: " + normalizedName);
        }

        Category parent = request.parentCategoryId() == null
                ? null
                : categoryRepository.findById(request.parentCategoryId())
                .orElseThrow(() -> new CatalogNotFoundException(
                        "Üst kategori bulunamadı: " + request.parentCategoryId()));

        Category category = Category.builder()
                .name(normalizedName)
                .description(trimToNull(request.description()))
                .parent(parent)
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(Long sellerId, CreateProductRequest request) {
        Set<Category> categories = loadCategories(request.categoryIds());
        validateSkus(request.variants());

        Product product = Product.builder()
                .sellerId(sellerId)
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .basePrice(request.basePrice())
                .status(ProductStatus.DRAFT)
                .categories(categories)
                .build();

        request.variants().stream()
                .map(requestVariant -> toVariant(product, requestVariant))
                .forEach(product.getVariants()::add);

        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductCatalogChangedEvent.now(saved.getId()));
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new CatalogNotFoundException("Ürün bulunamadı: " + productId));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, Long sellerId, UpdateProductRequest request) {
        Product product = productRepository.findDetailedById(productId)
                .filter(candidate -> candidate.getSellerId().equals(sellerId))
                .orElseThrow(() -> new CatalogNotFoundException("Satıcıya ait ürün bulunamadı: " + productId));

        if (request.description() != null) {
            product.setDescription(trimToNull(request.description()));
        }
        if (request.basePrice() != null) {
            product.setBasePrice(request.basePrice());
        }
        if (request.variants() != null) {
            updateVariants(product, request.variants());
        }

        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductCatalogChangedEvent.now(saved.getId()));
        return ProductResponse.from(saved);
    }

    private Set<Category> loadCategories(Set<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        Set<Long> foundIds = categories.stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> missingIds = new LinkedHashSet<>(categoryIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new CatalogNotFoundException("Kategoriler bulunamadı: " + missingIds);
        }
        return new LinkedHashSet<>(categories);
    }

    private void validateSkus(List<CreateProductVariantRequest> variants) {
        Set<String> uniqueSkus = new HashSet<>();
        for (CreateProductVariantRequest variant : variants) {
            String normalizedSku = normalizeSku(variant.sku());
            if (!uniqueSkus.add(normalizedSku)) {
                throw new CatalogConflictException("İstek içinde tekrarlanan SKU: " + normalizedSku);
            }
            if (productVariantRepository.existsBySku(normalizedSku)) {
                throw new CatalogConflictException("SKU zaten kullanılıyor: " + normalizedSku);
            }
        }
    }

    private ProductVariant toVariant(Product product, CreateProductVariantRequest request) {
        return ProductVariant.builder()
                .product(product)
                .sku(normalizeSku(request.sku()))
                .attributes(request.attributes())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();
    }

    private void updateVariants(Product product, List<UpdateProductVariantRequest> updates) {
        Map<Long, ProductVariant> variantsById = product.getVariants().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProductVariant::getId,
                        variant -> variant,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Set<Long> updatedIds = new HashSet<>();

        for (UpdateProductVariantRequest update : updates) {
            if (!updatedIds.add(update.id())) {
                throw new InvalidProductUpdateException("İstek içinde tekrarlanan varyant: " + update.id());
            }
            ProductVariant variant = variantsById.get(update.id());
            if (variant == null) {
                throw new CatalogNotFoundException("Ürüne ait varyant bulunamadı: " + update.id());
            }
            if (update.price() != null) {
                variant.setPrice(update.price());
            }
            if (update.stockQuantity() != null) {
                variant.setStockQuantity(update.stockQuantity());
            }
        }
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
