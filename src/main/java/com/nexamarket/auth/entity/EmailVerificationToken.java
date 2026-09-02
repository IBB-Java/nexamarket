package com.nexamarket.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@NoArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    private EmailVerificationToken(UserAccount user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static EmailVerificationToken issue(UserAccount user, String tokenHash, Instant expiresAt) {
        return new EmailVerificationToken(user, tokenHash, expiresAt);
    }

    public boolean isUsableAt(Instant now) {
        return verifiedAt == null && expiresAt.isAfter(now);
    }

    public void markVerified(Instant now) {
        this.verifiedAt = now;
    }
}
