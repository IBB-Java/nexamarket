package com.nexamarket.auth.api;

import com.nexamarket.auth.repository.EmailVerificationTokenRepository;
import com.nexamarket.auth.repository.RefreshTokenRepository;
import com.nexamarket.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.email-verification.required=true")
class UnverifiedRegistrationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private JavaMailSender mailSender;

    @AfterEach
    void cleanUp() {
        emailVerificationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void reissuesVerificationEmailWhenAnUnverifiedEmailRegistersAgain() throws Exception {
        String body = "{\"email\":\"pending@nexamarket.test\",\"password\":\"StrongPass!2026\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerified").value(false));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerified").value(false));

        assertEquals(1, userAccountRepository.count());
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void letsUnverifiedCustomerSignInAndVerifiesTheEmailWithTheDeliveredCode() throws Exception {
        String email = "code-flow@nexamarket.test";
        String password = "StrongPass!2026";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerified").value(false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<SimpleMailMessage> emailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailCaptor.capture());
        String code = emailCaptor.getValue().getText().split("\\n\\n")[1];

        mockMvc.perform(post("/api/v1/auth/verify-email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isNoContent());

        assertTrue(userAccountRepository.findByEmailIgnoreCase(email).orElseThrow().isEmailVerified());
    }
}
