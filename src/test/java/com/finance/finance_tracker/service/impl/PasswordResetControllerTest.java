package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.service.PasswordResetService;
import com.finance.finance_tracker.service.impl.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link PasswordResetController} через standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private LoginAttemptService loginAttemptService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PasswordResetController controller = new PasswordResetController(passwordResetService, loginAttemptService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /forgot-password возвращает форму")
    void forgotPasswordPage_returnsFormView() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"));
    }

    @Test
    @DisplayName("POST /forgot-password вызывает сервис с корректным base URL и показывает нейтральное сообщение")
    void forgotPassword_notBlocked_callsServiceAndShowsSuccessMessage() throws Exception {
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);

        mockMvc.perform(post("/forgot-password").param("email", "ivan@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("success"));

        // MockMvc по умолчанию: scheme=http, serverName=localhost, port=80 (дефолтный, суффикс не добавляется)
        verify(passwordResetService).requestReset(eq("ivan@example.com"), eq("http://localhost/reset-password"));
        verify(loginAttemptService).recordFailedAttempt(anyString());
    }

    @Test
    @DisplayName("POST /forgot-password при превышении лимита не вызывает сервис и пишет ошибку")
    void forgotPassword_blocked_doesNotCallServiceAndShowsError() throws Exception {
        when(loginAttemptService.isBlocked(anyString())).thenReturn(true);

        mockMvc.perform(post("/forgot-password").param("email", "ivan@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("error"));

        verify(passwordResetService, never()).requestReset(any(), any());
        verify(loginAttemptService, never()).recordFailedAttempt(anyString());
    }

    @Test
    @DisplayName("GET /reset-password с валидным токеном показывает форму")
    void resetPasswordPage_validToken_returnsFormView() throws Exception {
        mockMvc.perform(get("/reset-password").param("token", "good-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("token", "good-token"))
                .andExpect(model().attributeExists("resetPasswordDto"));
    }

    @Test
    @DisplayName("GET /reset-password с невалидным токеном показывает страницу ошибки")
    void resetPasswordPage_invalidToken_returnsInvalidView() throws Exception {
        doThrow(new InvalidDataException("Ссылка недействительна"))
                .when(passwordResetService).validateToken("bad-token");

        mockMvc.perform(get("/reset-password").param("token", "bad-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password-invalid"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /reset-password с валидными данными меняет пароль и редиректит на /login")
    void resetPassword_valid_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "good-token")
                        .param("newPassword", "newPassword123")
                        .param("confirmPassword", "newPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        verify(passwordResetService).resetPassword("good-token", "newPassword123");
    }

    @Test
    @DisplayName("POST /reset-password с несовпадающими паролями возвращает форму с ошибками")
    void resetPassword_mismatchedPasswords_returnsFormView() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "good-token")
                        .param("newPassword", "newPassword123")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("token", "good-token"));

        verify(passwordResetService, never()).resetPassword(any(), any());
    }

    @Test
    @DisplayName("POST /reset-password со слишком коротким паролем возвращает форму с ошибками")
    void resetPassword_tooShortPassword_returnsFormView() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "good-token")
                        .param("newPassword", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"));

        verify(passwordResetService, never()).resetPassword(any(), any());
    }

    @Test
    @DisplayName("POST /reset-password с истёкшим между GET и POST токеном показывает страницу ошибки")
    void resetPassword_serviceThrowsInvalidData_returnsInvalidView() throws Exception {
        doThrow(new InvalidDataException("Ссылка истекла"))
                .when(passwordResetService).resetPassword("expired-token", "newPassword123");

        mockMvc.perform(post("/reset-password")
                        .param("token", "expired-token")
                        .param("newPassword", "newPassword123")
                        .param("confirmPassword", "newPassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password-invalid"))
                .andExpect(model().attributeExists("error"));
    }
}