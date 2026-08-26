package com.nexamarket.catalog.search;

import com.nexamarket.catalog.entity.ProductStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "catalog.search.type", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchProductSearchGateway implements ProductSearchGateway {

    private final ElasticsearchOperations operations;
    private volatile boolean indexReady;

    public ElasticsearchProductSearchGateway(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void index(ProductSearchDocument document) {
        ensureIndex();
        operations.save(document);
    }

    @Override
    public ProductSearchPage search(ProductSearchCriteria searchCriteria) {
        ensureIndex();
        Criteria criteria = Criteria.where("status").is(ProductStatus.ACTIVE.name());

        if (searchCriteria.query() != null && !searchCriteria.query().isBlank()) {
            String queryText = searchCriteria.query().trim();
            Criteria textCriteria = Criteria.or()
                    .subCriteria(Criteria.where("name").matches(queryText).boost(3.0f))
                    .subCriteria(Criteria.where("description").matches(queryText))
                    .subCriteria(Criteria.where("categoryNames").matches(queryText));
            criteria = criteria.and(textCriteria);
        }
        if (searchCriteria.categoryId() != null) {
            criteria = criteria.and(Criteria.where("categoryIds").is(searchCriteria.categoryId()));
        }
        if (searchCriteria.minPrice() != null) {
            criteria = criteria.and(Criteria.where("maxPrice").greaterThanEqual(searchCriteria.minPrice()));
        }
        if (searchCriteria.maxPrice() != null) {
            criteria = criteria.and(Criteria.where("minPrice").lessThanEqual(searchCriteria.maxPrice()));
        }
        if (searchCriteria.minSellerRating() != null) {
            criteria = criteria.and(Criteria.where("sellerRating").greaterThanEqual(searchCriteria.minSellerRating()));
        }

        CriteriaQuery query = new CriteriaQuery(
                criteria,
                PageRequest.of(searchCriteria.page(), searchCriteria.size())
        );
        SearchHits<ProductSearchDocument> hits = operations.search(query, ProductSearchDocument.class);
        List<ProductSearchDocument> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        return new ProductSearchPage(
                items,
                hits.getTotalHits(),
                searchCriteria.page(),
                searchCriteria.size()
        );
    }

    private synchronized void ensureIndex() {
        if (indexReady) {
            return;
        }
        IndexOperations indexOperations = operations.indexOps(ProductSearchDocument.class);
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }
        indexReady = true;
    }
}
