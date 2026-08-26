package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.application.CreateReturnRequestCommand;
import com.nexamarket.nexamarket.order.application.ResolveReturnRequestCommand;
import com.nexamarket.nexamarket.order.application.ReturnRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnRequestView create(@Valid @RequestBody CreateReturnRequestRequest request) {
        return ReturnRequestView.from(returnRequestService.create(
                new CreateReturnRequestCommand(request.subOrderId(), request.reason())));
    }

    @PatchMapping("/{returnRequestId}")
    public ReturnRequestView resolve(@PathVariable UUID returnRequestId,
                                     @Valid @RequestBody ResolveReturnRequestRequest request) {
        return ReturnRequestView.from(returnRequestService.resolve(
                new ResolveReturnRequestCommand(returnRequestId, request.status(), request.resolverId())));
    }
}
