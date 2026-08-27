package com.nexamarket.nexamarket.cart.application;

import com.nexamarket.nexamarket.cart.domain.CartItem;
import com.nexamarket.nexamarket.cart.domain.CartStatus;
import com.nexamarket.nexamarket.cart.infrastructure.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class CartReservationExpirationService {

    private final CartItemRepository cartItemRepository;
    private final StockReservationGateway stockReservationGateway;
    private final Clock clock;

    @Autowired
    public CartReservationExpirationService(CartItemRepository cartItemRepository,
                                            StockReservationGateway stockReservationGateway) {
        this(cartItemRepository, stockReservationGateway, Clock.systemUTC());
    }

    CartReservationExpirationService(CartItemRepository cartItemRepository,
                                     StockReservationGateway stockReservationGateway, Clock clock) {
        this.cartItemRepository = cartItemRepository;
        this.stockReservationGateway = stockReservationGateway;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${cart.reservation-expiration.check-interval-ms}")
    @Transactional
    public void releaseExpiredReservationsOnSchedule() {
        releaseExpiredReservations();
    }

    /**
     * If Catalog Service cannot confirm a release, the item remains untouched and
     * will be retried by the next scheduled run.
     */
    @Transactional
    public int releaseExpiredReservations() {
        List<CartItem> expiredItems = cartItemRepository.findExpiredItemsForUpdate(Instant.now(clock), CartStatus.ACTIVE);
        for (CartItem item : expiredItems) {
            stockReservationGateway.releaseReservation(item.getReservationCode());
            item.getCart().removeItem(item);
            item.getCart().expireWhenEmpty();
        }
        return expiredItems.size();
    }
}
