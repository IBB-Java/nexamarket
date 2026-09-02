package com.nexamarket.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs automatically when Docker is available and proves Flyway migrations on
 * the same PostgreSQL major version used by compose. Local H2 tests remain fast.
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationContainerTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("nexamarket")
            .withUsername("nexamarket")
            .withPassword("nexamarket");

    @Test
    void appliesEveryMigrationOnPostgreSql() {
        var result = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(result.migrationsExecuted).isEqualTo(16);
    }
}
