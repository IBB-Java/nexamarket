package com.nexamarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Belgedeki p95 &lt; 300 ms kabul hedefi için tekrarlanabilir bir API smoke testi.
 * Uygulama başlangıcı ve JIT ısınması ölçüme dahil edilmez.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiPerformanceAcceptanceTest {

    private static final Duration P95_LIMIT = Duration.ofMillis(300);
    private static final int WARM_UP_REQUESTS = 10;
    private static final int MEASURED_REQUESTS = 50;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogSearchP95StaysBelowThreeHundredMilliseconds() throws Exception {
        for (int request = 0; request < WARM_UP_REQUESTS; request++) {
            performCatalogSearch();
        }

        List<Long> elapsedNanos = new ArrayList<>(MEASURED_REQUESTS);
        for (int request = 0; request < MEASURED_REQUESTS; request++) {
            long startedAt = System.nanoTime();
            performCatalogSearch();
            elapsedNanos.add(System.nanoTime() - startedAt);
        }

        Collections.sort(elapsedNanos);
        int p95Index = (int) Math.ceil(MEASURED_REQUESTS * 0.95) - 1;
        Duration measuredP95 = Duration.ofNanos(elapsedNanos.get(p95Index));

        assertThat(measuredP95)
                .as("catalog search p95 response time")
                .isLessThan(P95_LIMIT);
    }

    private void performCatalogSearch() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .queryParam("q", "nexamarket")
                        .queryParam("page", "0")
                        .queryParam("size", "20"))
                .andExpect(status().isOk());
    }
}
