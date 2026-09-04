package com.nexamarket.auth.application;

import com.nexamarket.auth.config.EmailVerificationProperties;
import com.nexamarket.auth.entity.EmailVerificationToken;
import com.nexamarket.auth.entity.UserAccount;
import com.nexamarket.auth.entity.UserStatus;
import com.nexamarket.auth.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final EmailVerificationProperties properties;

    @org.springframework.beans.factory.annotation.Value("${mail.from}")
    private String fromAddress;

    @Transactional
    public void sendVerification(UserAccount user) {
        if (!properties.isRequired() || user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }
        tokenRepository.deleteByUserId(user.getId());
        String verificationCode = createVerificationCode();
        tokenRepository.save(EmailVerificationToken.issue(user, hash(verificationCode),
                Instant.now().plus(properties.getTokenTtl())));
        sendEmail(user.getEmail(), verificationCode);
    }

    @Transactional
    public void verifyCode(String email, String verificationCode) {
        if (!properties.isRequired()) {
            return;
        }
        EmailVerificationToken token = tokenRepository.findByUserEmailAndTokenHashForUpdate(email.trim(), hash(verificationCode))
                .orElseThrow(InvalidEmailVerificationTokenException::new);
        if (!token.isUsableAt(Instant.now()) || token.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new InvalidEmailVerificationTokenException();
        }
        token.getUser().setEmailVerified(true);
        token.markVerified(Instant.now());
    }

    private void sendEmail(String recipient, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("NexaMarket doğrulama kodun");
        message.setText("NexaMarket hesabını doğrulamak için bu kodu uygulamadaki doğrulama alanına gir:\n\n"
                + verificationCode + "\n\nKod 24 saat boyunca geçerlidir. Bu isteği sen yapmadıysan bu e-postayı yok sayabilirsin.");
        mailSender.send(message);
    }

    private String createVerificationCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
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
