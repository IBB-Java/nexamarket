package com.nexamarket.catalog.application;

import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.catalog.api.ProductSearchResponse;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.search.ProductSearchCriteria;
import com.nexamarket.catalog.search.ProductSearchDocument;
import com.nexamarket.catalog.search.ProductSearchGateway;
import com.nexamarket.catalog.search.ProductSearchPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductSearchGateway productSearchGateway;
    @Mock
    private UserAccountRepository userAccountRepository;

    @Test
    void addsTheSellerEmailToSearchResults() {
        ProductSearchDocument document = ProductSearchDocument.builder()
                .id("301").sellerId(44L).name("Satıcı Bilgili Ürün").description("Açıklama")
                .status(ProductStatus.ACTIVE.name()).categoryIds(List.of()).categoryNames(List.of())
                .minPrice(10.0).maxPrice(10.0).totalStock(5L).inStock(true).build();
        UserAccount seller = UserAccount.builder().id(44L).email("seller@nexamarket.test").build();
        when(productSearchGateway.search(any())).thenReturn(new ProductSearchPage(List.of(document), 1, 0, 20));
        when(userAccountRepository.findAllById(any())).thenReturn(List.of(seller));
        ProductSearchService service = new ProductSearchService(productSearchGateway, userAccountRepository);

        ProductSearchResponse response = service.search(new ProductSearchCriteria(null, null, null, null, null, 0, 20));

        assertThat(response.items()).singleElement()
                .extracting(item -> item.sellerName())
                .isEqualTo("seller@nexamarket.test");
    }
}
