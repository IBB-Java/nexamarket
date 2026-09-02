package com.nexamarket.catalog.application;

import com.nexamarket.catalog.api.ProductSearchResponse;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductImage;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.repository.ProductImageRepository;
import com.nexamarket.catalog.search.ProductSearchCriteria;
import com.nexamarket.catalog.search.ProductSearchDocument;
import com.nexamarket.catalog.search.ProductSearchGateway;
import com.nexamarket.catalog.search.ProductSearchPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductSearchGateway productSearchGateway;
    @Mock
    private SellerDirectoryGateway sellerDirectoryGateway;
    @Mock
    private ProductImageRepository productImageRepository;

    @Test
    void addsSellerAndCoverImageInformationToSearchResults() {
        ProductSearchDocument document = ProductSearchDocument.builder()
                .id("301").sellerId(44L).name("Satıcı Bilgili Ürün").description("Açıklama")
                .status(ProductStatus.ACTIVE.name()).categoryIds(List.of()).categoryNames(List.of())
                .minPrice(10.0).maxPrice(10.0).totalStock(5L).inStock(true).build();
        when(productSearchGateway.search(any())).thenReturn(new ProductSearchPage(List.of(document), 1, 0, 20));
        when(sellerDirectoryGateway.displayNames(any())).thenReturn(Map.of(44L, "seller@nexamarket.test"));
        Product product = Product.builder().id(301L).build();
        ProductImage image = ProductImage.builder().id(81L).product(product).build();
        when(productImageRepository.findAllByProduct_IdInOrderByProduct_IdAscIdAsc(any())).thenReturn(List.of(image));
        ProductSearchService service = new ProductSearchService(productSearchGateway, sellerDirectoryGateway, productImageRepository);

        ProductSearchResponse response = service.search(new ProductSearchCriteria(null, null, null, null, null, 0, 20));

        assertThat(response.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.sellerName()).isEqualTo("seller@nexamarket.test");
                    assertThat(item.imageUrl()).isEqualTo("/api/v1/products/301/images/81/original");
                });
    }
}
