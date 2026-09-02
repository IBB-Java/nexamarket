package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.application.CreateReturnRequestCommand;
import com.nexamarket.nexamarket.order.application.ResolveReturnRequestCommand;
import com.nexamarket.nexamarket.order.application.ReturnRequestService;
import com.nexamarket.auth.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
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
    public ReturnRequestView create(@AuthenticationPrincipal AuthPrincipal principal,
                                    @Valid @RequestBody CreateReturnRequestRequest request) {
        return ReturnRequestView.from(returnRequestService.create(
                new CreateReturnRequestCommand(request.subOrderId(), request.reason()), principal));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<ReturnRequestView> listMine(@AuthenticationPrincipal AuthPrincipal principal) {
        return returnRequestService.listForCustomer(principal.userId()).stream().map(ReturnRequestView::from).toList();
    }

    @GetMapping("/manageable")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public List<ReturnRequestView> listManageable(@AuthenticationPrincipal AuthPrincipal principal) {
        return returnRequestService.listManageableBy(principal).stream().map(ReturnRequestView::from).toList();
    }

    @PatchMapping("/{returnRequestId}")
    public ReturnRequestView resolve(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID returnRequestId,
                                     @Valid @RequestBody ResolveReturnRequestRequest request) {
        return ReturnRequestView.from(returnRequestService.resolve(
                new ResolveReturnRequestCommand(returnRequestId, request.status(), principal.userId()), principal));
    }
}
