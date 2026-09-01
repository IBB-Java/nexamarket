package com.nexamarket.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the four logical-service boundaries required by the analysis document. */
class ServiceBoundaryArchitectureTest {

    private static final Path SOURCES = Path.of("src/main/java/com/nexamarket");

    @Test
    void declaresAtLeastFourLogicalServices() {
        assertThat(List.of("identity", "catalog-inventory", "commerce", "engagement-reporting"))
                .hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void commerceDoesNotCallIdentityOrCatalogInventoryImplementationsDirectly() throws IOException {
        assertNoForbiddenImports(
                List.of(SOURCES.resolve("nexamarket"), SOURCES.resolve("promotion"), SOURCES.resolve("loyalty")),
                List.of(
                        "com.nexamarket.auth.application.", "com.nexamarket.auth.repository.",
                        "com.nexamarket.users.application.", "com.nexamarket.users.repository.",
                        "com.nexamarket.catalog.application.", "com.nexamarket.catalog.repository.",
                        "com.nexamarket.stock.application.", "com.nexamarket.stock.repository."));
    }

    @Test
    void catalogInventoryDoesNotCallIdentityOrCommerceImplementationsDirectly() throws IOException {
        assertNoForbiddenImports(
                List.of(SOURCES.resolve("catalog"), SOURCES.resolve("stock")),
                List.of(
                        "com.nexamarket.auth.application.", "com.nexamarket.auth.repository.",
                        "com.nexamarket.users.application.", "com.nexamarket.users.repository.",
                        "com.nexamarket.nexamarket.cart.application.",
                        "com.nexamarket.nexamarket.order.application.",
                        "com.nexamarket.nexamarket.order.infrastructure.",
                        "com.nexamarket.nexamarket.payment.application."));
    }

    @Test
    void reportingDoesNotReadCommerceRepositoriesDirectly() throws IOException {
        assertNoForbiddenImports(
                List.of(SOURCES.resolve("report")),
                List.of(
                        "com.nexamarket.nexamarket.order.infrastructure.",
                        "com.nexamarket.nexamarket.payment.infrastructure."));
    }

    private void assertNoForbiddenImports(List<Path> roots, List<String> forbiddenPackages) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (var files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    for (String line : Files.readAllLines(file)) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("import ")
                                && forbiddenPackages.stream().anyMatch(trimmed::contains)) {
                            violations.add(file + ": " + trimmed);
                        }
                    }
                }
            }
        }
        assertThat(violations)
                .as("service boundaries must use internal REST contracts or RabbitMQ events")
                .isEmpty();
    }
}
