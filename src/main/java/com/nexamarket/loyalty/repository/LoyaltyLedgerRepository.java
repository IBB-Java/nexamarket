package com.nexamarket.loyalty.repository;

import com.nexamarket.loyalty.entity.LoyaltyEntryType;
import com.nexamarket.loyalty.entity.LoyaltyLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoyaltyLedgerRepository extends JpaRepository<LoyaltyLedgerEntry, Long> {
    boolean existsBySubOrderIdAndType(UUID subOrderId, LoyaltyEntryType type);

    Optional<LoyaltyLedgerEntry> findBySubOrderIdAndType(UUID subOrderId, LoyaltyEntryType type);

    @Query("select coalesce(sum(entry.points), 0) from LoyaltyLedgerEntry entry where entry.customerId = :customerId")
    Integer totalPointsByCustomerId(@Param("customerId") Long customerId);
}
