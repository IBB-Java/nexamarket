package com.nexamarket.auth.security;

import com.nexamarket.auth.config.AuthProperties;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;

class JwtServiceTest {

    @Test
    void acceptsUrlSafeBase64SecretsFromEnvironmentFiles() {
        AuthProperties properties = new AuthProperties();
        String raw = "nexamarket-url-safe-secret-with-enough-entropy-2026";
        String urlSafeSecret = Encoders.BASE64URL.encode(raw.getBytes(StandardCharsets.UTF_8));
        properties.getJwt().setSecret(urlSafeSecret);

        assertThatCode(() -> new JwtService(properties)).doesNotThrowAnyException();
    }
}
