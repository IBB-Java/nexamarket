package com.nexamarket.loyalty.application;

import com.nexamarket.loyalty.entity.LoyaltyEntryType;
import com.nexamarket.loyalty.entity.LoyaltyLedgerEntry;
import com.nexamarket.loyalty.repository.LoyaltyLedgerRepository;
import com.nexamarket.nexamarket.order.domain.SubOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock
    private LoyaltyLedgerRepository loyaltyLedgerRepository;

    @Test
    void awardsOnePointForEveryPurchasedItem() {
        SubOrder subOrder = purchasedSubOrder(3, 2);
        when(loyaltyLedgerRepository.existsBySubOrderIdAndType(subOrder.getId(), LoyaltyEntryType.EARNED)).thenReturn(false);

        service().awardForPaidPurchase(subOrder);

        ArgumentCaptor<LoyaltyLedgerEntry> saved = ArgumentCaptor.forClass(LoyaltyLedgerEntry.class);
        verify(loyaltyLedgerRepository).save(saved.capture());
        assertThat(saved.getValue().getPoints()).isEqualTo(5);
        assertThat(saved.getValue().getType()).isEqualTo(LoyaltyEntryType.EARNED);
        assertThat(saved.getValue().getDescription()).isEqualTo("Paid order items");
    }

    @Test
    void approvedReturnWritesANegativeLedgerEntry() {
        SubOrder subOrder = mock(SubOrder.class);
        when(subOrder.getId()).thenReturn(UUID.randomUUID());
        LoyaltyLedgerEntry earned = new LoyaltyLedgerEntry(801L, subOrder.getId(), LoyaltyEntryType.EARNED,
                5, "Paid order items", Instant.parse("2026-08-27T08:00:00Z"));
        when(loyaltyLedgerRepository.existsBySubOrderIdAndType(subOrder.getId(), LoyaltyEntryType.REVERSED)).thenReturn(false);
        when(loyaltyLedgerRepository.findBySubOrderIdAndType(subOrder.getId(), LoyaltyEntryType.EARNED))
                .thenReturn(Optional.of(earned));

        service().reverseForApprovedReturn(subOrder);

        ArgumentCaptor<LoyaltyLedgerEntry> saved = ArgumentCaptor.forClass(LoyaltyLedgerEntry.class);
        verify(loyaltyLedgerRepository).save(saved.capture());
        assertThat(saved.getValue().getPoints()).isEqualTo(-5);
        assertThat(saved.getValue().getType()).isEqualTo(LoyaltyEntryType.REVERSED);
    }

    private LoyaltyService service() {
        return new LoyaltyService(loyaltyLedgerRepository,
                Clock.fixed(Instant.parse("2026-08-27T09:00:00Z"), ZoneOffset.UTC));
    }

    private SubOrder purchasedSubOrder(int... quantities) {
        var order = mock(com.nexamarket.nexamarket.order.domain.CustomerOrder.class);
        SubOrder subOrder = mock(SubOrder.class);
        when(order.getCustomerId()).thenReturn(801L);
        when(subOrder.getOrder()).thenReturn(order);
        when(subOrder.getId()).thenReturn(UUID.randomUUID());
        var items = Arrays.stream(quantities).mapToObj(quantity -> {
            var item = mock(com.nexamarket.nexamarket.order.domain.OrderItem.class);
            when(item.getQuantity()).thenReturn(quantity);
            return item;
        }).toList();
        when(subOrder.getItems()).thenReturn(items);
        return subOrder;
    }
}
