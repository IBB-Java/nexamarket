package com.nexamarket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads optional machine-specific settings from the project-root .env file.
 * The file is intentionally ignored by Git so SMTP credentials never enter source control.
 */
@Configuration
@Profile("!test")
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class LocalEnvironmentConfiguration {
}
