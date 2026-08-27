package com.nexamarket.loyalty.application;

import com.nexamarket.loyalty.entity.LoyaltyEntryType;
import com.nexamarket.loyalty.entity.LoyaltyLedgerEntry;
import com.nexamarket.loyalty.repository.LoyaltyLedgerRepository;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

/** Awards one point per ten currency units only after delivery. */
@Service
public class LoyaltyService {

    private final LoyaltyLedgerRepository loyaltyLedgerRepository;
    private final Clock clock;

    @Autowired
    public LoyaltyService(LoyaltyLedgerRepository loyaltyLedgerRepository) {
        this(loyaltyLedgerRepository, Clock.systemUTC());
    }

    LoyaltyService(LoyaltyLedgerRepository loyaltyLedgerRepository, Clock clock) {
        this.loyaltyLedgerRepository = loyaltyLedgerRepository;
        this.clock = clock;
    }

    @Transactional
    public void awardForDelivery(SubOrder subOrder) {
        if (loyaltyLedgerRepository.existsBySubOrderIdAndType(subOrder.getId(), LoyaltyEntryType.EARNED)) {
            return;
        }
        int points = subOrder.getSubtotal().divide(new BigDecimal("10"), 0, RoundingMode.DOWN).intValue();
        if (points == 0) {
            return;
        }
        loyaltyLedgerRepository.save(new LoyaltyLedgerEntry(subOrder.getOrder().getCustomerId(), subOrder.getId(),
                LoyaltyEntryType.EARNED, points, "Delivered sub-order", Instant.now(clock)));
    }

    @Transactional
    public void reverseForApprovedReturn(SubOrder subOrder) {
        if (loyaltyLedgerRepository.existsBySubOrderIdAndType(subOrder.getId(), LoyaltyEntryType.REVERSED)) {
            return;
        }
        loyaltyLedgerRepository.findBySubOrderIdAndType(subOrder.getId(), LoyaltyEntryType.EARNED)
                .ifPresent(earned -> loyaltyLedgerRepository.save(new LoyaltyLedgerEntry(
                        earned.getCustomerId(), subOrder.getId(), LoyaltyEntryType.REVERSED, -earned.getPoints(),
                        "Approved return reversal", Instant.now(clock))));
    }

    @Transactional(readOnly = true)
    public int balance(Long customerId) {
        return loyaltyLedgerRepository.totalPointsByCustomerId(customerId);
    }
}
