package com.nexamarket.promotion.entity;

import com.nexamarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "promotions")
@Getter
@NoArgsConstructor
public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Column(name = "rule_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Column(name = "minimum_order_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(nullable = false)
    private boolean stackable;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    public Promotion(String code, PromotionType type, BigDecimal value, BigDecimal minimumOrderAmount,
                     boolean stackable, Instant startsAt, Instant endsAt) {
        this.code = normalizeCode(code);
        this.type = type;
        this.value = value;
        this.minimumOrderAmount = minimumOrderAmount == null ? BigDecimal.ZERO : minimumOrderAmount;
        this.stackable = stackable;
        this.active = true;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        validate();
    }

    public boolean isUsableAt(Instant now, BigDecimal subtotal) {
        return active && !now.isBefore(startsAt) && !now.isAfter(endsAt)
                && subtotal.compareTo(minimumOrderAmount) >= 0;
    }

    public BigDecimal discountFor(BigDecimal subtotal) {
        return switch (type) {
            case PERCENTAGE -> subtotal.multiply(value).movePointLeft(2);
            case FIXED_AMOUNT -> value.min(subtotal);
        };
    }

    private void validate() {
        if (code == null || code.isBlank() || type == null || value == null || value.signum() <= 0
                || minimumOrderAmount.signum() < 0 || startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Kampanya tanımı geçersiz.");
        }
        if (type == PromotionType.PERCENTAGE && value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Yüzde kampanya değeri 100'ü geçemez.");
        }
    }

    private String normalizeCode(String input) {
        return input == null ? null : input.trim().toUpperCase(Locale.ROOT);
    }
}
