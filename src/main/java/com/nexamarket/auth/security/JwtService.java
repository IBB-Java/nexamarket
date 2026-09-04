package com.nexamarket.auth.security;

import com.nexamarket.auth.application.InvalidTokenException;
import com.nexamarket.auth.config.AuthProperties;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";

    private final AuthProperties properties;
    private final SecretKey signingKey;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        String encodedSecret = properties.getJwt().getSecret();
        byte[] keyBytes = encodedSecret.indexOf('-') >= 0 || encodedSecret.indexOf('_') >= 0
                ? Decoders.BASE64URL.decode(encodedSecret)
                : Decoders.BASE64.decode(encodedSecret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public IssuedJwt createAccessToken(UserAccount user) {
        return createToken(user, ACCESS_TOKEN, properties.getJwt().getAccessTokenTtl());
    }

    public IssuedJwt createRefreshToken(UserAccount user) {
        return createToken(user, REFRESH_TOKEN, properties.getJwt().getRefreshTokenTtl());
    }

    public JwtPayload parseAccessToken(String token) {
        return parse(token, ACCESS_TOKEN);
    }

    public JwtPayload parseRefreshToken(String token) {
        return parse(token, REFRESH_TOKEN);
    }

    private IssuedJwt createToken(UserAccount user, String tokenType, java.time.Duration ttl) {
        Instant now = Instant.now();
        Instant expiration = now.plus(ttl);
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(user.getEmail())
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("tokenType", tokenType)
                .signWith(signingKey)
                .compact();
        return new IssuedJwt(token, tokenId, expiration);
    }

    private JwtPayload parse(String token, String expectedTokenType) {
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = parsed.getPayload();
            String tokenType = claims.get("tokenType", String.class);
            if (!expectedTokenType.equals(tokenType)) {
                throw new InvalidTokenException();
            }
            Object userIdClaim = claims.get("userId");
            Long userId = userIdClaim instanceof Number number ? number.longValue() : null;
            String role = claims.get("role", String.class);
            if (userId == null || role == null || claims.getId() == null) {
                throw new InvalidTokenException();
            }
            return new JwtPayload(
                    claims.getId(), userId, claims.getSubject(), UserRole.valueOf(role), tokenType,
                    claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException();
        }
    }
}
