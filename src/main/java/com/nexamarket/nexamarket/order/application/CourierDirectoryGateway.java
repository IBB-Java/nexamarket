package com.nexamarket.nexamarket.order.application;

public interface CourierDirectoryGateway {

    boolean isActiveCourier(Long userId);

    java.util.List<Long> findActiveCourierIds();
}
