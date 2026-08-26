package com.nexamarket.catalog.search;

import com.nexamarket.catalog.entity.ProductStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "catalog.search.type", havingValue = "memory")
public class InMemoryProductSearchGateway implements ProductSearchGateway {

    private final Map<String, ProductSearchDocument> documents = new ConcurrentHashMap<>();

    @Override
    public void index(ProductSearchDocument document) {
        documents.put(document.getId(), document);
    }

    @Override
    public ProductSearchPage search(ProductSearchCriteria criteria) {
        List<ProductSearchDocument> matches = documents.values().stream()
                .filter(document -> ProductStatus.ACTIVE.name().equals(document.getStatus()))
                .filter(document -> matchesText(document, criteria.query()))
                .filter(document -> criteria.categoryId() == null
                        || document.getCategoryIds().contains(criteria.categoryId()))
                .filter(document -> overlapsPriceRange(document, criteria.minPrice(), criteria.maxPrice()))
                .filter(document -> criteria.minSellerRating() == null
                        || document.getSellerRating() != null
                        && document.getSellerRating() >= criteria.minSellerRating().doubleValue())
                .sorted(Comparator.comparing(ProductSearchDocument::getName)
                        .thenComparing(ProductSearchDocument::getId))
                .toList();

        int fromIndex = Math.min(criteria.page() * criteria.size(), matches.size());
        int toIndex = Math.min(fromIndex + criteria.size(), matches.size());
        return new ProductSearchPage(matches.subList(fromIndex, toIndex), matches.size(), criteria.page(), criteria.size());
    }

    private boolean matchesText(ProductSearchDocument document, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return contains(document.getName(), normalized)
                || contains(document.getDescription(), normalized)
                || document.getCategoryNames().stream().anyMatch(name -> contains(name, normalized));
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private boolean overlapsPriceRange(ProductSearchDocument document, BigDecimal minPrice, BigDecimal maxPrice) {
        boolean aboveMinimum = minPrice == null || document.getMaxPrice() >= minPrice.doubleValue();
        boolean belowMaximum = maxPrice == null || document.getMinPrice() <= maxPrice.doubleValue();
        return aboveMinimum && belowMaximum;
    }
}
