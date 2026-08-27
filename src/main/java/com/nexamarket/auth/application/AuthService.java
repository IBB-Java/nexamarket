package com.nexamarket.auth.application;

import com.nexamarket.auth.api.CreateUserRequest;
import com.nexamarket.auth.api.LoginRequest;
import com.nexamarket.auth.api.RegisterRequest;
import com.nexamarket.auth.api.TokenResponse;
import com.nexamarket.auth.api.UserResponse;
import com.nexamarket.auth.config.AuthProperties;
import com.nexamarket.auth.entity.RefreshToken;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.RefreshTokenRepository;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.auth.security.IssuedJwt;
import com.nexamarket.auth.security.JwtPayload;
import com.nexamarket.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    @Transactional
    public UserResponse registerCustomer(RegisterRequest request) {
        return UserResponse.from(createUser(normalizeEmail(request.email()), request.password(), UserRole.CUSTOMER));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return UserResponse.from(createUser(normalizeEmail(request.email()), request.password(), request.role()));
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        unlockIfExpired(user);
        rejectUnavailableAccount(user);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedLogin(user);
            throw new InvalidCredentialsException();
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        JwtPayload payload = jwtService.parseRefreshToken(rawRefreshToken);
        RefreshToken current = refreshTokenRepository.findForUpdateByTokenId(payload.tokenId())
                .orElseThrow(InvalidTokenException::new);
        if (!MessageDigest.isEqual(hash(rawRefreshToken).getBytes(StandardCharsets.US_ASCII),
                current.getTokenHash().getBytes(StandardCharsets.US_ASCII))
                || current.getRevokedAt() != null
                || current.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException();
        }
        UserAccount user = current.getUser();
        if (!user.getId().equals(payload.userId()) || !user.getEmail().equalsIgnoreCase(payload.email())) {
            throw new InvalidTokenException();
        }
        unlockIfExpired(user);
        rejectUnavailableAccount(user);

        current.setRevokedAt(Instant.now());
        TokenResponse replacement = issueTokenPair(user);
        JwtPayload replacementPayload = jwtService.parseRefreshToken(replacement.refreshToken());
        current.setReplacedByTokenId(replacementPayload.tokenId());
        return replacement;
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        JwtPayload payload = jwtService.parseRefreshToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findForUpdateByTokenId(payload.tokenId())
                .orElseThrow(InvalidTokenException::new);
        if (!MessageDigest.isEqual(hash(rawRefreshToken).getBytes(StandardCharsets.US_ASCII),
                token.getTokenHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new InvalidTokenException();
        }
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(InvalidTokenException::new);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userAccountRepository.findAll().stream().map(UserResponse::from).toList();
    }

    private UserAccount createUser(String email, String password, UserRole role) {
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("Bu e-posta adresi zaten kayıtlı");
        }
        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        return userAccountRepository.save(user);
    }

    private TokenResponse issueTokenPair(UserAccount user) {
        IssuedJwt access = jwtService.createAccessToken(user);
        IssuedJwt refresh = jwtService.createRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenId(refresh.tokenId())
                .tokenHash(hash(refresh.value()))
                .user(user)
                .expiresAt(refresh.expiresAt())
                .build());
        return new TokenResponse(
                "Bearer",
                access.value(),
                authProperties.getJwt().getAccessTokenTtl().toSeconds(),
                refresh.value(),
                authProperties.getJwt().getRefreshTokenTtl().toSeconds(),
                user.getRole());
    }

    private void recordFailedLogin(UserAccount user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= authProperties.getLogin().getMaxFailedAttempts()) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(Instant.now().plus(authProperties.getLogin().getLockDuration()));
        }
    }

    private void unlockIfExpired(UserAccount user) {
        if (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null
                && !user.getLockedUntil().isAfter(Instant.now())) {
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
    }

    private void rejectUnavailableAccount(UserAccount user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException(user.getLockedUntil());
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountDisabledException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algoritması bulunamadı", exception);
        }
    }
}
