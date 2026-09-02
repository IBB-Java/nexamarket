package com.nexamarket.stock.application;

import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.catalog.entity.Product;
import com.nexamarket.catalog.entity.ProductStatus;
import com.nexamarket.catalog.entity.ProductVariant;
import com.nexamarket.catalog.repository.ProductRepository;
import com.nexamarket.catalog.repository.ProductVariantRepository;
import com.nexamarket.stock.api.CreateStockReservationRequest;
import com.nexamarket.stock.api.StockReservationResponse;
import com.nexamarket.stock.repository.StockReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class StockServiceIntegrationTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @AfterEach
    void cleanUp() {
        stockReservationRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void expiresReservationsAndReturnsTheirStock() {
        ProductVariant variant = createVariant(2);
        StockReservationResponse reservation = stockService.reserve(
                new CreateStockReservationRequest(variant.getId(), 2), customer());
        assertEquals(0, reservation.availableStock());

        var persistedReservation = stockReservationRepository.findAll().getFirst();
        persistedReservation.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).minusSeconds(1));
        stockReservationRepository.saveAndFlush(persistedReservation);

        assertEquals(1, stockService.expireReservations());
        assertEquals(2, stockService.getStockLevel(variant.getId()).availableStock());
        assertEquals("EXPIRED", stockReservationRepository.findAll().getFirst().getStatus().name());
    }

    @Test
    void preventsOversellingDuringConcurrentReservations() throws Exception {
        ProductVariant variant = createVariant(10);
        int requestCount = 30;
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int request = 0; request < requestCount; request++) {
            tasks.add(() -> {
                startSignal.await(10, TimeUnit.SECONDS);
                try {
                    stockService.reserve(new CreateStockReservationRequest(variant.getId(), 1), customer());
                    return true;
                } catch (InsufficientStockException exception) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> results = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            results.add(executor.submit(task));
        }
        startSignal.countDown();

        int successfulReservations = 0;
        for (Future<Boolean> result : results) {
            if (result.get(20, TimeUnit.SECONDS)) {
                successfulReservations++;
            }
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(10, successfulReservations);
        assertEquals(0, stockService.getStockLevel(variant.getId()).availableStock());
        assertEquals(10, stockReservationRepository.count());
    }

    @Test
    void supportsIncreasingAndDecreasingAnActiveInternalReservation() {
        ProductVariant variant = createVariant(5);
        StockReservationResponse created = stockService.reserve(
                new CreateStockReservationRequest(variant.getId(), 1), customer());

        StockReservationResponse increased = stockService.increaseReservationInternally(
                created.reservationCode(), 2);
        assertEquals(3, increased.quantity());
        assertEquals(2, increased.availableStock());

        StockReservationResponse decreased = stockService.decreaseReservationInternally(
                created.reservationCode(), 1);
        assertEquals(2, decreased.quantity());
        assertEquals(3, decreased.availableStock());

        stockService.confirmReservationInternally(created.reservationCode());
        stockService.confirmReservationInternally(created.reservationCode());
        stockService.releaseReservationInternally(created.reservationCode());

        assertEquals("CONFIRMED", stockReservationRepository.findAll().getFirst().getStatus().name());
        assertEquals(3, stockService.getStockLevel(variant.getId()).availableStock());
    }

    @Test
    void rejectsInvalidIncreaseAndAccessFromAnotherCustomer() {
        ProductVariant variant = createVariant(3);
        StockReservationResponse created = stockService.reserve(
                new CreateStockReservationRequest(variant.getId(), 1), customer());

        assertThrows(IllegalArgumentException.class,
                () -> stockService.increaseReservationInternally(created.reservationCode(), 0));
        assertThrows(StockAccessDeniedException.class,
                () -> stockService.release(created.reservationCode(),
                        new AuthPrincipal(100L, "other@nexamarket.test", UserRole.CUSTOMER)));
    }

    private ProductVariant createVariant(int stockQuantity) {
        Product product = productRepository.save(Product.builder()
                .name("Eşzamanlılık Test Ürünü " + stockQuantity)
                .description("Flaş indirim stoğu")
                .basePrice(BigDecimal.TEN)
                .sellerId(1L)
                .status(ProductStatus.ACTIVE)
                .build());
        return productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("CONCURRENT-STOCK-" + stockQuantity)
                .price(BigDecimal.TEN)
                .stockQuantity(stockQuantity)
                .build());
    }

    private AuthPrincipal customer() {
        return new AuthPrincipal(99L, "stock-customer@nexamarket.test", UserRole.CUSTOMER);
    }
}
