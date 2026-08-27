package com.nexamarket.nexamarket.payment.infrastructure;

import com.nexamarket.nexamarket.payment.domain.WalletAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from WalletAccount wallet where wallet.customerId = :customerId")
    Optional<WalletAccount> findByCustomerIdForUpdate(@Param("customerId") Long customerId);
}
