package com.finance.finance_tracker.controller.advice;

import com.finance.finance_tracker.dto.UserSettingsDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Theme;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ThemeControllerAdvice}.
 */
@ExtendWith(MockitoExtension.class)
class ThemeControllerAdviceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private ThemeControllerAdvice themeControllerAdvice;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("неаутентифицированный пользователь получает LIGHT по умолчанию")
    void currentTheme_notAuthenticated_returnsLight() {
        SecurityContextHolder.clearContext();

        String result = themeControllerAdvice.currentTheme();

        assertThat(result).isEqualTo("LIGHT");
    }

    @Test
    @DisplayName("аутентифицированный пользователь получает свою реальную тему")
    void currentTheme_authenticated_returnsUserTheme() {
        authenticateAs(1L);
        UserSettingsDto settings = new UserSettingsDto();
        settings.setTheme(Theme.DARK);
        when(userService.getUserSettings(1L)).thenReturn(settings);

        String result = themeControllerAdvice.currentTheme();

        assertThat(result).isEqualTo("DARK");
    }

    @Test
    @DisplayName("если сервис настроек бросает исключение — fallback на LIGHT, а не 500 на каждой странице")
    void currentTheme_serviceThrows_fallsBackToLight() {
        authenticateAs(999L);
        when(userService.getUserSettings(999L)).thenThrow(new EntityNotFoundException("Пользователь не найден"));

        String result = themeControllerAdvice.currentTheme();

        assertThat(result).isEqualTo("LIGHT");
    }
}