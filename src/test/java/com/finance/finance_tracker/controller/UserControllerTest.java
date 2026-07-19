package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.service.Impl.UserDetailsServiceImpl;
import com.finance.finance_tracker.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link UserController} через standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private UserDetailsServiceImpl userDetailsServiceImpl;

    private MockMvc mockMvc;
    private UserDto currentUserDto;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService, userDetailsServiceImpl);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPassword("encodedOld");
        user.setActive(true);
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        currentUserDto = new UserDto();
        currentUserDto.setId(1L);
        currentUserDto.setEmail("user@example.com");
        currentUserDto.setName("Иван");
        currentUserDto.setSurname("Иванов");
        currentUserDto.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /profile возвращает профиль пользователя вместе со счетами")
    void userProfile_authenticated_returnsProfileView() throws Exception {
        when(userService.getUserByEmail("user@example.com")).thenReturn(currentUserDto);
        when(userService.getUserAccounts(1L)).thenReturn(List.of(new AccountDto()));

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"));
    }

    @Test
    @DisplayName("GET /edit возвращает форму редактирования профиля")
    void editProfilePage_returnsEditView() throws Exception {
        when(userService.getUserByEmail("user@example.com")).thenReturn(currentUserDto);

        mockMvc.perform(get("/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/edit"));
    }

    @Test
    @DisplayName("POST /edit с валидными данными обновляет профиль и редиректит")
    void updateProfile_valid_redirectsToProfile() throws Exception {
        when(userService.getUserByEmail("user@example.com")).thenReturn(currentUserDto);
        when(userService.updateUser(anyLong(), any(UserDto.class))).thenReturn(currentUserDto);

        mockMvc.perform(post("/edit")
                        .param("name", "Иван")
                        .param("surname", "Иванов")
                        .param("birthday", "1990-01-01")
                        .param("email", "user@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/profile"));
    }

    @Test
    @DisplayName("POST /edit с некорректным email возвращает форму с ошибками")
    void updateProfile_invalidEmail_returnsEditView() throws Exception {
        mockMvc.perform(post("/edit")
                        .param("name", "Иван")
                        .param("surname", "Иванов")
                        .param("birthday", "1990-01-01")
                        .param("email", "not-an-email")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/edit"));

        verify(userService, never()).updateUser(anyLong(), any());
    }

    @Test
    @DisplayName("GET /change-password возвращает страницу смены пароля")
    void changePasswordPage_returnsView() throws Exception {
        mockMvc.perform(get("/change-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/change-password"));
    }

    @Test
    @DisplayName("POST /change-password с совпадающими паролями меняет пароль и редиректит")
    void changePassword_success_redirects() throws Exception {
        when(userService.getUserByEmail("user@example.com")).thenReturn(currentUserDto);

        mockMvc.perform(post("/change-password")
                        .param("currentPassword", "oldPass")
                        .param("newPassword", "newPassword123")
                        .param("confirmPassword", "newPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/profile"));

        verify(userService).changePassword(1L, "oldPass", "newPassword123");
    }

    @Test
    @DisplayName("POST /change-password с несовпадающим подтверждением возвращает форму с ошибкой")
    void changePassword_mismatchedConfirmation_returnsViewWithError() throws Exception {
        mockMvc.perform(post("/change-password")
                        .param("currentPassword", "oldPass")
                        .param("newPassword", "newPassword123")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/change-password"));

        verify(userService, never()).changePassword(anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST /change-password при неверном текущем пароле возвращает форму с ошибкой сервиса")
    void changePassword_wrongCurrentPassword_returnsViewWithServiceError() throws Exception {
        when(userService.getUserByEmail("user@example.com")).thenReturn(currentUserDto);
        doThrow(new InvalidDataException("Текущий пароль введён неверно"))
                .when(userService).changePassword(1L, "wrongPass", "newPassword123");

        mockMvc.perform(post("/change-password")
                        .param("currentPassword", "wrongPass")
                        .param("newPassword", "newPassword123")
                        .param("confirmPassword", "newPassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/change-password"));
    }
}
