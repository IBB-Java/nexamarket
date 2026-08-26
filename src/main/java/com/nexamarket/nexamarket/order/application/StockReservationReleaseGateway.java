package com.nexamarket.nexamarket.order.application;

import java.util.UUID;

/** Order Service releases reservations through Catalog Service's REST boundary. */
public interface StockReservationReleaseGateway {

    void releaseReservation(UUID reservationId);
}
