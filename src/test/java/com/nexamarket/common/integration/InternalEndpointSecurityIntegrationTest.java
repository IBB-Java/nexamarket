package com.nexamarket.common.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalEndpointSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMissingServiceKeyAndAcceptsTheConfiguredKey() throws Exception {
        mockMvc.perform(get("/internal/identity/users").queryParam("ids", "999999"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/internal/identity/users")
                        .queryParam("ids", "999999")
                        .header(InternalApiKeyFilter.HEADER, "test-internal-api-key"))
                .andExpect(status().isOk());
    }
}
