package com.nexamarket.auth.application;

import com.nexamarket.auth.config.EmailVerificationProperties;
import com.nexamarket.auth.entity.EmailVerificationToken;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private JavaMailSender mailSender;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setRequired(true);
        properties.setTokenTtl(Duration.ofHours(24));
        service = new EmailVerificationService(tokenRepository, mailSender, properties);
        ReflectionTestUtils.setField(service, "publicBaseUrl", "http://localhost:8080/");
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@nexamarket.local");
    }

    @Test
    void sendsVerificationLinkAndStoresOnlyTheTokenHash() {
        UserAccount user = unverifiedUser();
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sendVerification(user);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).deleteByUserId(user.getId());
        verify(tokenRepository).save(tokenCaptor.capture());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        SimpleMailMessage message = mailCaptor.getValue();
        String rawToken = message.getText().split("token=")[1].split("\\n")[0];

        assertThat(message.getTo()).containsExactly(user.getEmail());
        assertThat(message.getSubject()).contains("doğrulaması");
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(sha256(rawToken));
        assertThat(tokenCaptor.getValue().getTokenHash()).doesNotContain(rawToken);
        assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(Instant.now().plus(Duration.ofHours(23)));
    }

    @Test
    void verifiesAUsableTokenAndActivatesTheAccount() {
        String rawToken = "known-verification-token";
        UserAccount user = unverifiedUser();
        EmailVerificationToken token = EmailVerificationToken.issue(
                user, sha256(rawToken), Instant.now().plus(Duration.ofHours(1)));
        when(tokenRepository.findByTokenHashForUpdate(sha256(rawToken))).thenReturn(Optional.of(token));

        service.verify(rawToken);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getVerifiedAt()).isNotNull();
    }

    private UserAccount unverifiedUser() {
        return UserAccount.builder()
                .id(42L)
                .email("hilal@nexamarket.test")
                .passwordHash("encoded-password")
                .emailVerified(false)
                .build();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
