package com.nexamarket.promotion.application;

import java.math.BigDecimal;
import java.util.List;

public record PromotionQuote(BigDecimal discountAmount, List<String> appliedCodes) {
    public static PromotionQuote none() {
        return new PromotionQuote(BigDecimal.ZERO, List.of());
    }
}
