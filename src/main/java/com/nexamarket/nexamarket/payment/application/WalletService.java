package com.nexamarket.nexamarket.payment.application;

import com.nexamarket.nexamarket.payment.domain.WalletAccount;
import com.nexamarket.nexamarket.payment.infrastructure.WalletAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletAccountRepository walletAccountRepository;

    public WalletService(WalletAccountRepository walletAccountRepository) {
        this.walletAccountRepository = walletAccountRepository;
    }

    /** Internal development endpoint support; real wallet funding belongs to a regulated provider. */
    @Transactional
    public BigDecimal credit(Long customerId, BigDecimal amount) {
        if (customerId == null || amount == null || amount.signum() <= 0) {
            throw new PaymentException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Customer id and a positive credit amount are required.");
        }
        WalletAccount wallet = walletAccountRepository.findByCustomerIdForUpdate(customerId)
                .orElseGet(() -> WalletAccount.open(customerId, BigDecimal.ZERO));
        wallet.credit(amount);
        return walletAccountRepository.save(wallet).getBalance();
    }
}
