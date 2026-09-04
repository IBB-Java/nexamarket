package com.nexamarket.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.auth.entity.RefreshToken;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.RefreshTokenRepository;
import com.nexamarket.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void registersCustomerHashesPasswordAndIssuesAccessAndRefreshTokens() throws Exception {
        register("customer@nexamarket.test", "StrongPass!2026");

        UserAccount stored = userAccountRepository.findByEmailIgnoreCase("customer@nexamarket.test").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("StrongPass!2026", stored.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("StrongPass!2026", stored.getPasswordHash()));

        JsonNode tokens = login("customer@nexamarket.test", "StrongPass!2026");
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@nexamarket.test"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void letsAUserChooseCustomerSellerOrCourierButNeverAdminDuringRegistration() throws Exception {
        for (String role : java.util.List.of("CUSTOMER", "SELLER", "COURIER")) {
            String email = role.toLowerCase() + "-self@nexamarket.test";
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"StrongPass!2026\",\"role\":\"" + role + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value(role));
        }

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin-self@nexamarket.test\",\"password\":\"StrongPass!2026\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Kayıt rolüne izin verilmiyor"));
        org.assertj.core.api.Assertions.assertThat(
                userAccountRepository.findByEmailIgnoreCase("admin-self@nexamarket.test")).isEmpty();
    }

    @Test
    void rejectsUnauthenticatedProfileRequestAndDuplicateRegistration() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        register("duplicate@nexamarket.test", "StrongPass!2026");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"duplicate@nexamarket.test\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Kayıt çakışması"));
    }

    @Test
    void rotatesAndRevokesRefreshTokens() throws Exception {
        register("rotate@nexamarket.test", "StrongPass!2026");
        JsonNode first = login("rotate@nexamarket.test", "StrongPass!2026");
        String firstRefresh = first.get("refreshToken").asText();

        String refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String secondRefresh = objectMapper.readTree(refreshed).get("refreshToken").asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(firstRefresh, secondRefresh);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + secondRefresh + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + secondRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void locksAccountAfterConfiguredFailedLoginCount() throws Exception {
        register("locked@nexamarket.test", "StrongPass!2026");
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"locked@nexamarket.test\",\"password\":\"WrongPass!2026\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"locked@nexamarket.test\",\"password\":\"StrongPass!2026\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.title").value("Hesap geçici olarak kilitli"));

        UserAccount user = userAccountRepository.findByEmailIgnoreCase("locked@nexamarket.test").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.LOCKED, user.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(user.getLockedUntil());
    }

    @Test
    void restrictsAdminEndpointsByRole() throws Exception {
        register("customer-role@nexamarket.test", "StrongPass!2026");
        String customerAccess = login("customer-role@nexamarket.test", "StrongPass!2026").get("accessToken").asText();

        UserAccount admin = userAccountRepository.save(UserAccount.builder()
                .email("admin@nexamarket.test")
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        String adminAccess = login(admin.getEmail(), "StrongPass!2026").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/auth/users")
                        .header("Authorization", "Bearer " + customerAccess))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/auth/users")
                        .header("Authorization", "Bearer " + adminAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"seller@nexamarket.test","password":"StrongPass!2026","role":"SELLER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
