package com.nexamarket.auth.repository;

import com.nexamarket.auth.entity.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from EmailVerificationToken token join fetch token.user where token.tokenHash = :tokenHash")
    Optional<EmailVerificationToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    void deleteByUserId(Long userId);
}
