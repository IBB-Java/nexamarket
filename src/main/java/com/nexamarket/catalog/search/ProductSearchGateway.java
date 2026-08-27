package com.nexamarket.catalog.search;

public interface ProductSearchGateway {
    void index(ProductSearchDocument document);

    ProductSearchPage search(ProductSearchCriteria criteria);
}
