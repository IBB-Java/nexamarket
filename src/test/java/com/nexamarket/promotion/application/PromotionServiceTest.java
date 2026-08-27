package com.nexamarket.promotion.application;

import com.nexamarket.promotion.entity.Promotion;
import com.nexamarket.promotion.entity.PromotionType;
import com.nexamarket.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-27T09:00:00Z");

    @Mock
    private PromotionRepository promotionRepository;

    @Test
    void capsEvenALargeEligibleDiscountAtSeventyPercent() {
        Promotion campaign = new Promotion("BIG70", PromotionType.FIXED_AMOUNT, new BigDecimal("95.00"),
                BigDecimal.ZERO, false, NOW.minusSeconds(60), NOW.plusSeconds(60));
        when(promotionRepository.findByCodeIn(java.util.Set.of("BIG70"))).thenReturn(List.of(campaign));

        PromotionQuote quote = service().quote(List.of("big70"), new BigDecimal("100.00"));

        assertThat(quote.discountAmount()).isEqualByComparingTo("70.00");
        assertThat(quote.appliedCodes()).containsExactly("BIG70");
    }

    private PromotionService service() {
        return new PromotionService(promotionRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
