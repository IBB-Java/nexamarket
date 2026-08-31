package com.nexamarket.users.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserRole;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.RefreshTokenRepository;
import com.nexamarket.auth.repository.UserAccountRepository;
import com.nexamarket.users.repository.SellerProfileRepository;
import com.nexamarket.users.repository.UserProfileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsersApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private SellerProfileRepository sellerProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        sellerProfileRepository.deleteAll();
        userProfileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void updatesOnlyTheAuthenticatedUsersProfile() throws Exception {
        String accessToken = registerAndLogin("profile@nexamarket.test", "StrongPass!2026");

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ayşe","lastName":"Yılmaz","phoneNumber":"+905551112233"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile@nexamarket.test"))
                .andExpect(jsonPath("$.firstName").value("Ayşe"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Yılmaz"));
    }

    @Test
    void requiresAdminApprovalBeforeSellerProfileIsPublic() throws Exception {
        String customerAccess = registerAndLogin("customer@nexamarket.test", "StrongPass!2026");
        UserAccount admin = userAccountRepository.save(UserAccount.builder()
                .email("admin@nexamarket.test")
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        String adminAccess = login(admin.getEmail(), "StrongPass!2026");

        mockMvc.perform(post("/api/v1/admin/auth/users")
                        .header("Authorization", "Bearer " + adminAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"seller@nexamarket.test","password":"StrongPass!2026","role":"SELLER"}
                                """))
                .andExpect(status().isCreated());
        String sellerAccess = login("seller@nexamarket.test", "StrongPass!2026");

        mockMvc.perform(post("/api/v1/sellers/me")
                        .header("Authorization", "Bearer " + customerAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Müşteri Mağazası\"}"))
                .andExpect(status().isForbidden());

        String sellerBody = mockMvc.perform(post("/api/v1/sellers/me")
                        .header("Authorization", "Bearer " + sellerAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName":"Nexa Elektronik","description":"Elektronik ürünler"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        long sellerId = objectMapper.readTree(sellerBody).get("id").asLong();

        mockMvc.perform(get("/api/v1/sellers/{sellerId}", sellerId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/sellers/pending")
                        .header("Authorization", "Bearer " + adminAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sellerId));
        mockMvc.perform(patch("/api/v1/admin/sellers/{sellerId}/status", sellerId)
                        .header("Authorization", "Bearer " + adminAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/sellers/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Nexa Elektronik"));
    }

    @Test
    void allowsAdminToDisableUserProfileAccess() throws Exception {
        String customerAccess = registerAndLogin("disabled@nexamarket.test", "StrongPass!2026");
        UserAccount customer = userAccountRepository.findByEmailIgnoreCase("disabled@nexamarket.test").orElseThrow();
        UserAccount admin = userAccountRepository.save(UserAccount.builder()
                .email("admin-disable@nexamarket.test")
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        String adminAccess = login(admin.getEmail(), "StrongPass!2026");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", customer.getId())
                        .header("Authorization", "Bearer " + adminAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customerAccess))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanPromoteRegisteredCustomerToSeller() throws Exception {
        String customerEmail = "promote@nexamarket.test";
        String customerAccess = registerAndLogin(customerEmail, "StrongPass!2026");
        UserAccount customer = userAccountRepository.findByEmailIgnoreCase(customerEmail).orElseThrow();
        UserAccount admin = userAccountRepository.save(UserAccount.builder()
                .email("admin-promote@nexamarket.test")
                .passwordHash(passwordEncoder.encode("StrongPass!2026"))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        String adminAccess = login(admin.getEmail(), "StrongPass!2026");

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", customer.getId())
                        .header("Authorization", "Bearer " + customerAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SELLER\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/role", customer.getId())
                        .header("Authorization", "Bearer " + adminAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SELLER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SELLER"));

        String sellerAccess = login(customerEmail, "StrongPass!2026");
        mockMvc.perform(post("/api/v1/sellers/me")
                        .header("Authorization", "Bearer " + sellerAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Yükseltilen Mağaza\"}"))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        return response.get("accessToken").asText();
    }
}
