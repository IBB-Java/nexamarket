package com.nexamarket.promotion.api;

import com.nexamarket.promotion.entity.Promotion;
import com.nexamarket.promotion.entity.PromotionType;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionResponse(Long id, String code, PromotionType type, BigDecimal value,
                                BigDecimal minimumOrderAmount, boolean stackable, boolean active,
                                Instant startsAt, Instant endsAt) {
    static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getCode(), promotion.getType(), promotion.getValue(),
                promotion.getMinimumOrderAmount(), promotion.isStackable(), promotion.isActive(),
                promotion.getStartsAt(), promotion.getEndsAt());
    }
}
