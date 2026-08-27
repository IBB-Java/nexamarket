package com.nexamarket.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Login login = new Login();

    public Jwt getJwt() {
        return jwt;
    }

    public Login getLogin() {
        return login;
    }

    public static class Jwt {
        private String secret = "VGVzdC1Pbmx5LU5leGFNYXJrZXQtSldULVNlY3JldC1LZXktMzItQnl0ZXMtTWlu";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(30);

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public Duration getAccessTokenTtl() { return accessTokenTtl; }
        public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
        public Duration getRefreshTokenTtl() { return refreshTokenTtl; }
        public void setRefreshTokenTtl(Duration refreshTokenTtl) { this.refreshTokenTtl = refreshTokenTtl; }
    }

    public static class Login {
        private int maxFailedAttempts = 5;
        private Duration lockDuration = Duration.ofMinutes(15);

        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }
        public Duration getLockDuration() { return lockDuration; }
        public void setLockDuration(Duration lockDuration) { this.lockDuration = lockDuration; }
    }
}
