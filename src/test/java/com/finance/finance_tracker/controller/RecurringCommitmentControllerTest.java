package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.dto.RecurringCommitmentDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.RecurringCommitmentService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
 * Тесты {@link RecurringCommitmentController} через standalone MockMvc.
 *
 * <p>Написан после того, как обнаружилось, что recurring/list.html и
 * recurring/form.html ссылались на несуществующий путь
 * {@code /templates/recurring/...} вместо {@code /recurring/...} — фича
 * была рабочей на уровне сервиса, но недостижимой из UI (404 на
 * "Добавить"/toggle/delete), и это не ловилось, потому что теста на
 * контроллер не было вообще. Этот класс закрывает пробел.
 */
@ExtendWith(MockitoExtension.class)
class RecurringCommitmentControllerTest {

    @Mock
    private RecurringCommitmentService recurringCommitmentService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RecurringCommitmentController controller =
                new RecurringCommitmentController(recurringCommitmentService, categoryService, accountService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        authenticateAsUserId(1L, "user@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsUserId(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setActive(true);
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /recurring возвращает список плановых списаний пользователя")
    void list_returnsListView() throws Exception {
        RecurringCommitmentDto dto = new RecurringCommitmentDto();
        dto.setId(1L);
        when(recurringCommitmentService.getByUserId(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/recurring"))
                .andExpect(status().isOk())
                .andExpect(view().name("recurring/list"))
                .andExpect(model().attribute("commitments", List.of(dto)));
    }

    @Test
    @DisplayName("GET /recurring/create возвращает форму создания с категориями, счетами и типами")
    void createForm_returnsFormViewWithModel() throws Exception {
        when(categoryService.getAllCategoriesByUserId(1L)).thenReturn(List.of(new CategoryDto()));
        when(accountService.getUserAccounts(1L)).thenReturn(List.of(new AccountDto()));

        mockMvc.perform(get("/recurring/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("recurring/form"))
                .andExpect(model().attributeExists("commitmentDto", "categories", "accounts", "types"));
    }

    @Test
    @DisplayName("POST /recurring с валидными данными сохраняет и редиректит на /recurring")
    void save_valid_redirectsToRecurring() throws Exception {
        mockMvc.perform(post("/recurring")
                        .param("name", "Netflix")
                        .param("amount", "799.00")
                        .param("type", "EXPENSE")
                        .param("dayOfMonth", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurring"))
                .andExpect(flash().attributeExists("success"));

        verify(recurringCommitmentService).save(any(RecurringCommitmentDto.class), eqUserId());
    }

    @Test
    @DisplayName("POST /recurring без обязательных полей возвращает форму с ошибками")
    void save_invalid_returnsFormView() throws Exception {
        when(categoryService.getAllCategoriesByUserId(1L)).thenReturn(List.of());
        when(accountService.getUserAccounts(1L)).thenReturn(List.of());

        mockMvc.perform(post("/recurring").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("recurring/form"));
    }

    @Test
    @DisplayName("POST /recurring/{id}/toggle переключает статус и редиректит на /recurring")
    void toggle_success_redirectsToRecurring() throws Exception {
        mockMvc.perform(post("/recurring/5/toggle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurring"));

        verify(recurringCommitmentService).toggleActive(5L, 1L);
    }

    @Test
    @DisplayName("POST /recurring/{id}/toggle при ошибке сервиса пишет flash-ошибку")
    void toggle_serviceThrows_setsFlashError() throws Exception {
        doThrow(new RuntimeException("Нет доступа"))
                .when(recurringCommitmentService).toggleActive(5L, 1L);

        mockMvc.perform(post("/recurring/5/toggle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurring"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /recurring/{id}/delete удаляет и редиректит на /recurring")
    void delete_success_redirectsToRecurring() throws Exception {
        mockMvc.perform(post("/recurring/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurring"))
                .andExpect(flash().attributeExists("success"));

        verify(recurringCommitmentService).delete(5L, 1L);
    }

    @Test
    @DisplayName("POST /recurring/{id}/delete при ошибке сервиса пишет flash-ошибку")
    void delete_serviceThrows_setsFlashError() throws Exception {
        doThrow(new RuntimeException("Плановое списание не найдено"))
                .when(recurringCommitmentService).delete(404L, 1L);

        mockMvc.perform(post("/recurring/404/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recurring"))
                .andExpect(flash().attributeExists("error"));
    }

    private Long eqUserId() {
        return 1L;
    }
}