package com.nexamarket.nexamarket.order.api;

import com.nexamarket.nexamarket.order.application.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @GetMapping
    public List<AdminDeliveryAssignmentResponse> list() {
        return deliveryAssignmentService.listAll();
    }

    @PostMapping("/assign")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDeliveryAssignmentResponse assign(@Valid @RequestBody CreateDeliveryAssignmentRequest request) {
        return deliveryAssignmentService.assign(request.subOrderId(), request.courierId());
    }
}
