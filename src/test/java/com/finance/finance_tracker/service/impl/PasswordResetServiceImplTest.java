package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.entity.PasswordResetToken;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.repository.PasswordResetTokenRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link PasswordResetServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("ivan@example.com");
        user.setPassword("encodedOldPassword");
    }

    private PasswordResetToken validToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setToken("good-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        token.setUsed(false);
        return token;
    }

    @Nested
    @DisplayName("requestReset")
    class RequestReset {

        @Test
        @DisplayName("для существующего email создаёт токен и отправляет письмо")
        void requestReset_existingEmail_createsTokenAndSendsEmail() {
            when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));

            passwordResetService.requestReset("ivan@example.com", "http://localhost:8080/reset-password");

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            PasswordResetToken saved = tokenCaptor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(29))
                    .isBefore(LocalDateTime.now().plusMinutes(31));

            verify(emailService).sendPasswordResetEmail(
                    eq("ivan@example.com"),
                    argThat(link -> link.startsWith("http://localhost:8080/reset-password?token=" + saved.getToken())));
        }

        @Test
        @DisplayName("сгенерированный токен URL-safe (без +, / и padding =)")
        void requestReset_generatedToken_isUrlSafe() {
            when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));

            passwordResetService.requestReset("ivan@example.com", "http://localhost:8080/reset-password");

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());

            assertThat(tokenCaptor.getValue().getToken()).doesNotContain("+", "/", "=");
        }

        @Test
        @DisplayName("для незарегистрированного email не создаёт токен и не шлёт письмо")
        void requestReset_unknownEmail_doesNothing() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            passwordResetService.requestReset("unknown@example.com", "http://localhost:8080/reset-password");

            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(any(), any());
        }

        @Test
        @DisplayName("токен всё равно сохранён, даже если отправка письма упала с исключением")
        void requestReset_emailSendingFails_tokenStillSaved() {
            when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
            doThrow(new RuntimeException("SMTP недоступен"))
                    .when(emailService).sendPasswordResetEmail(anyString(), anyString());

            passwordResetService.requestReset("ivan@example.com", "http://localhost:8080/reset-password");

            verify(tokenRepository).save(any(PasswordResetToken.class));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("валидный токен — не бросает исключение")
        void validateToken_validToken_doesNotThrow() {
            when(tokenRepository.findByToken("good-token")).thenReturn(Optional.of(validToken()));

            assertDoesNotThrow(() -> passwordResetService.validateToken("good-token"));
        }

        @Test
        @DisplayName("бросает InvalidDataException, если токен не найден")
        void validateToken_notFound_throws() {
            when(tokenRepository.findByToken("missing")).thenReturn(Optional.empty());

            assertThrows(InvalidDataException.class, () -> passwordResetService.validateToken("missing"));
        }

        @Test
        @DisplayName("бросает InvalidDataException, если токен истёк")
        void validateToken_expired_throws() {
            PasswordResetToken token = validToken();
            token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

            assertThrows(InvalidDataException.class, () -> passwordResetService.validateToken("expired"));
        }

        @Test
        @DisplayName("бросает InvalidDataException, если токен уже использован")
        void validateToken_alreadyUsed_throws() {
            PasswordResetToken token = validToken();
            token.setUsed(true);
            when(tokenRepository.findByToken("used")).thenReturn(Optional.of(token));

            assertThrows(InvalidDataException.class, () -> passwordResetService.validateToken("used"));
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("устанавливает новый пароль и помечает токен использованным")
        void resetPassword_validToken_updatesPasswordAndMarksTokenUsed() {
            PasswordResetToken token = validToken();
            when(tokenRepository.findByToken("good-token")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNew");

            passwordResetService.resetPassword("good-token", "newPassword123");

            assertThat(user.getPassword()).isEqualTo("encodedNew");
            verify(userRepository).save(user);
            assertThat(token.isUsed()).isTrue();
            verify(tokenRepository).save(token);
        }

        @Test
        @DisplayName("бросает InvalidDataException для несуществующего токена и не трогает пользователя")
        void resetPassword_tokenNotFound_throwsAndDoesNotTouchUser() {
            when(tokenRepository.findByToken("missing")).thenReturn(Optional.empty());

            assertThrows(InvalidDataException.class,
                    () -> passwordResetService.resetPassword("missing", "newPassword123"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает InvalidDataException для уже использованного токена")
        void resetPassword_usedToken_throws() {
            PasswordResetToken token = validToken();
            token.setUsed(true);
            when(tokenRepository.findByToken("used")).thenReturn(Optional.of(token));

            assertThrows(InvalidDataException.class,
                    () -> passwordResetService.resetPassword("used", "newPassword123"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает InvalidDataException для истёкшего токена")
        void resetPassword_expiredToken_throws() {
            PasswordResetToken token = validToken();
            token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

            assertThrows(InvalidDataException.class,
                    () -> passwordResetService.resetPassword("expired", "newPassword123"));
            verify(userRepository, never()).save(any());
        }
    }
}