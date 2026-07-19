package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.service.BudgetService;
import com.finance.finance_tracker.service.CategoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link BudgetController} через standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    @Mock
    private BudgetService budgetService;
    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BudgetController controller = new BudgetController(budgetService, categoryService);

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
    @DisplayName("GET /budgets без аутентификации редиректит на /login")
    void listBudgets_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/budgets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /budgets возвращает список бюджетов текущего пользователя")
    void listBudgets_authenticated_returnsListView() throws Exception {
        when(budgetService.getBudgetsByUserId(1L)).thenReturn(List.of(new BudgetDto()));

        mockMvc.perform(get("/budgets"))
                .andExpect(status().isOk())
                .andExpect(view().name("budgets/list"));
    }

    @Test
    @DisplayName("GET /budgets/create возвращает форму создания")
    void showCreateForm_returnsFormView() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/budgets/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("budgets/form"));
    }

    @Test
    @DisplayName("ИЗВЕСТНЫЙ БАГ: POST /budgets всегда сохраняет бюджет под userId=1, игнорируя текущего пользователя")
    void saveBudget_alwaysUsesHardcodedUserId() throws Exception {
        // аутентифицируемся как пользователь с id=42, а не 1
        SecurityContextHolder.clearContext();
        authenticateAsUserId(42L, "other@example.com");

        mockMvc.perform(post("/budgets")
                        .param("categoryId", "5")
                        .param("monthlyLimit", "1000.00")
                        .param("currentSpending", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/budgets"));

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(budgetService).saveBudget(any(BudgetDto.class), userIdCaptor.capture());
        // Ожидалось бы 42L (текущий пользователь), но контроллер хардкодит 1L — баг зафиксирован тестом
        assertThat(userIdCaptor.getValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("POST /budgets без обязательных полей возвращает форму с ошибками")
    void saveBudget_missingRequiredFields_returnsFormView() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(post("/budgets").param("monthlyLimit", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("budgets/form"));
    }

    @Test
    @DisplayName("POST /budgets/{id}/reset сбрасывает траты и редиректит")
    void resetBudget_success_redirects() throws Exception {
        mockMvc.perform(post("/budgets/1/reset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/budgets"))
                .andExpect(flash().attributeExists("success"));

        verify(budgetService).resetSpending(1L);
    }

    @Test
    @DisplayName("POST /budgets/{id}/reset при ошибке пишет flash-ошибку, но не падает")
    void resetBudget_serviceThrows_setsFlashError() throws Exception {
        doThrow(new RuntimeException("Бюджет не найден")).when(budgetService).resetSpending(404L);

        mockMvc.perform(post("/budgets/404/reset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /budgets/{id}/delete удаляет бюджет и редиректит")
    void deleteBudget_success_redirects() throws Exception {
        mockMvc.perform(post("/budgets/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/budgets"))
                .andExpect(flash().attributeExists("success"));

        verify(budgetService).deleteBudget(1L);
    }

    @Test
    @DisplayName("POST /budgets/{id}/delete при ошибке пишет flash-ошибку")
    void deleteBudget_serviceThrows_setsFlashError() throws Exception {
        doThrow(new RuntimeException("Бюджет не найден")).when(budgetService).deleteBudget(404L);

        mockMvc.perform(post("/budgets/404/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
}
