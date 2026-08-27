package com.nexamarket.promotion.application;

import com.nexamarket.promotion.entity.Promotion;
import com.nexamarket.promotion.entity.PromotionType;
import com.nexamarket.promotion.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Data-driven campaign rules. Several campaigns may be combined only when all
 * of them are stackable; the final discount is always capped at 70 percent.
 */
@Service
public class PromotionService {

    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.70");

    private final PromotionRepository promotionRepository;
    private final Clock clock;

    @Autowired
    public PromotionService(PromotionRepository promotionRepository) {
        this(promotionRepository, Clock.systemUTC());
    }

    PromotionService(PromotionRepository promotionRepository, Clock clock) {
        this.promotionRepository = promotionRepository;
        this.clock = clock;
    }

    @Transactional
    public Promotion create(String code, PromotionType type, BigDecimal value, BigDecimal minimumOrderAmount,
                            boolean stackable, Instant startsAt, Instant endsAt) {
        String normalized = normalize(code);
        if (promotionRepository.findByCode(normalized).isPresent()) {
            throw new IllegalArgumentException("Bu kampanya kodu zaten tanımlı.");
        }
        return promotionRepository.save(new Promotion(normalized, type, value, minimumOrderAmount,
                stackable, startsAt, endsAt));
    }

    @Transactional(readOnly = true)
    public PromotionQuote quote(List<String> requestedCodes, BigDecimal subtotal) {
        if (requestedCodes == null || requestedCodes.isEmpty()) {
            return PromotionQuote.none();
        }
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Sepet ara toplamı geçersiz.");
        }

        Set<String> codes = new LinkedHashSet<>();
        requestedCodes.forEach(code -> codes.add(normalize(code)));
        List<Promotion> promotions = promotionRepository.findByCodeIn(codes);
        if (promotions.size() != codes.size()) {
            throw new IllegalArgumentException("Kampanya kodlarından en az biri bulunamadı.");
        }
        Instant now = Instant.now(clock);
        if (promotions.stream().anyMatch(promotion -> !promotion.isUsableAt(now, subtotal))) {
            throw new IllegalArgumentException("Kampanya koşulları bu sepet için sağlanmıyor.");
        }
        if (promotions.size() > 1 && promotions.stream().anyMatch(promotion -> !promotion.isStackable())) {
            throw new IllegalArgumentException("Birleştirilemeyen kampanyalar birlikte kullanılamaz.");
        }

        BigDecimal rawDiscount = promotions.stream()
                .map(promotion -> promotion.discountFor(subtotal))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cappedDiscount = rawDiscount.min(subtotal.multiply(MAX_DISCOUNT_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        return new PromotionQuote(cappedDiscount, List.copyOf(codes));
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Kampanya kodu boş olamaz.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
