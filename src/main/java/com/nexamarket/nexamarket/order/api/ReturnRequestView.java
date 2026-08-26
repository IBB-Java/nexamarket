package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.domain.ReturnRequest;
import com.nexamarket.nexamarket.order.domain.ReturnRequestStatus;

import java.util.UUID;

public record ReturnRequestView(UUID id, ReturnRequestStatus status) {

    static ReturnRequestView from(ReturnRequest returnRequest) {
        return new ReturnRequestView(returnRequest.getId(), returnRequest.getStatus());
    }
}
