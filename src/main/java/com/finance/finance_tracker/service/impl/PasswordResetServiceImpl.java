package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.entity.PasswordResetToken;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.repository.PasswordResetTokenRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.EmailService;
import com.finance.finance_tracker.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_VALID_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void requestReset(String email, String resetLinkBase) {
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            String token = generateToken();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES));
            tokenRepository.save(resetToken);

            try {
                emailService.sendPasswordResetEmail(email, resetLinkBase + "?token=" + token);
            } catch (Exception e) {
                // Не пробрасываем — пользователь в любом случае видит одно и
                // то же сообщение, независимо от реального успеха отправки.
                log.error("Не удалось отправить письмо сброса пароля на {}: {}", email, e.getMessage(), e);
            }

            log.info("Создан токен сброса пароля: userId={}", user.getId());
        }, () -> log.debug("Запрос сброса для незарегистрированного email"));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateToken(String token) {
        findValidToken(token);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = findValidToken(token);

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Пароль сброшен по токену: userId={}", user.getId());
    }

    private PasswordResetToken findValidToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidDataException("Ссылка для сброса пароля недействительна"));

        if (!resetToken.isValid()) {
            throw new InvalidDataException("Ссылка для сброса пароля недействительна или истекла");
        }
        return resetToken;
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}