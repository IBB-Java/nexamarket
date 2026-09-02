package com.nexamarket.auth.application;

import com.nexamarket.auth.config.EmailVerificationProperties;
import com.nexamarket.auth.entity.EmailVerificationToken;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final EmailVerificationProperties properties;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @Value("${mail.from}")
    private String fromAddress;

    @Transactional
    public void sendVerification(UserAccount user) {
        if (!properties.isRequired() || user.isEmailVerified()) {
            return;
        }
        tokenRepository.deleteByUserId(user.getId());
        String rawToken = createRawToken();
        tokenRepository.save(EmailVerificationToken.issue(user, hash(rawToken),
                Instant.now().plus(properties.getTokenTtl())));
        sendEmail(user.getEmail(), rawToken);
    }

    @Transactional
    public void verify(String rawToken) {
        if (!properties.isRequired()) {
            return;
        }
        EmailVerificationToken token = tokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidEmailVerificationTokenException::new);
        if (!token.isUsableAt(Instant.now())) {
            throw new InvalidEmailVerificationTokenException();
        }
        token.getUser().setEmailVerified(true);
        token.markVerified(Instant.now());
    }

    private void sendEmail(String recipient, String rawToken) {
        String link = publicBaseUrl.replaceAll("/$", "") + "/api/v1/auth/verify-email?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("NexaMarket e-posta doğrulaması");
        message.setText("NexaMarket hesabını etkinleştirmek için aşağıdaki bağlantıyı aç:\n\n" + link
                + "\n\nBu bağlantı 24 saat boyunca geçerlidir.");
        mailSender.send(message);
    }

    private String createRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("E-posta doğrulama anahtarı oluşturulamadı", exception);
        }
    }
}
