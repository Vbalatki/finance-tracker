package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link AuthController}. Страницы логина/регистрации публичны,
 * поэтому имитировать SecurityContext не требуется.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /login возвращает страницу логина")
    void loginPage_returnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /register возвращает страницу регистрации")
    void registerPage_returnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    @DisplayName("POST /register с валидными данными регистрирует пользователя и редиректит на /login")
    void register_valid_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Иван")
                        .param("surname", "Иванов")
                        .param("birthday", "1990-01-01")
                        .param("email", "ivan@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService).registerUser(any(UserDto.class));
    }

    @Test
    @DisplayName("POST /register с некорректным email возвращает форму с ошибками")
    void register_invalidEmail_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Иван")
                        .param("surname", "Иванов")
                        .param("birthday", "1990-01-01")
                        .param("email", "not-an-email")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    @DisplayName("POST /register при уже существующем email возвращает форму с ошибкой поля")
    void register_duplicateEmail_returnsRegisterViewWithFieldError() throws Exception {
        doThrow(new DuplicateEntityException("Email уже зарегистрирован"))
                .when(userService).registerUser(any(UserDto.class));

        mockMvc.perform(post("/register")
                        .param("name", "Иван")
                        .param("surname", "Иванов")
                        .param("birthday", "1990-01-01")
                        .param("email", "ivan@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }
}
