package com.nexamarket.promotion.api;

import com.nexamarket.promotion.application.PromotionService;
import com.nexamarket.promotion.entity.Promotion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only campaign management; the security rule is /api/v1/admin/**. */
@RestController
@RequestMapping("/api/v1/admin/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse create(@Valid @RequestBody CreatePromotionRequest request) {
        Promotion promotion = promotionService.create(request.code(), request.type(), request.value(),
                request.minimumOrderAmount(), request.stackable(), request.startsAt(), request.endsAt());
        return PromotionResponse.from(promotion);
    }
}
