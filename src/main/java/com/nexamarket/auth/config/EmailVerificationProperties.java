package com.nexamarket.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.email-verification")
public class EmailVerificationProperties {
    private boolean required = true;
    private Duration tokenTtl = Duration.ofHours(24);
}
