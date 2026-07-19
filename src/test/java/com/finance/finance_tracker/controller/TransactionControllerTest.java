package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.handler.GlobalExceptionHandler;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.TransactionService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link TransactionController} через standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private AccountService accountService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TransactionController controller =
                new TransactionController(accountService, categoryService, transactionService, userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
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
    @DisplayName("GET /transactions без аутентификации редиректит на /login")
    void transactionsPage_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /transactions возвращает список транзакций")
    void transactionsPage_authenticated_returnsListView() throws Exception {
        when(accountService.getUserAccounts(1L)).thenReturn(List.of());
        when(userService.getUserTotalBalanceInRub(List.of())).thenReturn(BigDecimal.ZERO);
        when(userService.getUserTotalIncomeInRub(List.of())).thenReturn(BigDecimal.ZERO);
        when(userService.getUserTotalExpenseInRub(List.of())).thenReturn(BigDecimal.ZERO);
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/list"));
    }

    @Test
    @DisplayName("GET /transactions/create возвращает форму создания")
    void createTransactionPage_returnsCreateView() throws Exception {
        when(accountService.getUserAccounts(1L)).thenReturn(List.of());
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/transactions/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/create"));
    }

    @Test
    @DisplayName("POST /transactions с валидными данными создаёт транзакцию и редиректит")
    void createTransaction_valid_redirectsToTransactions() throws Exception {
        mockMvc.perform(post("/transactions")
                        .param("amount", "100.00")
                        .param("type", "INCOME")
                        .param("accountId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions"));

        verify(transactionService).saveTransaction(any(TransactionDto.class));
    }

    @Test
    @DisplayName("POST /transactions без суммы возвращает форму с ошибками")
    void createTransaction_blankAmount_returnsCreateView() throws Exception {
        when(accountService.getUserAccounts(1L)).thenReturn(List.of());
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(post("/transactions")
                        .param("amount", "")
                        .param("accountId", "10")
                        .param("type", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/create"));

        verify(transactionService, never()).saveTransaction(any());
    }

    @Test
    @DisplayName("GET /transactions/{id}/edit для своей транзакции возвращает JSON")
    void getTransactionEditForm_ownTransaction_returnsJson() throws Exception {
        TransactionDto existing = new TransactionDto();
        existing.setId(1L);
        existing.setAccountId(10L);
        existing.setAmount(new BigDecimal("50.00"));
        existing.setType(TransactionType.EXPENSE);

        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(1L);

        when(transactionService.getTransactionById(1L)).thenReturn(existing);
        when(accountService.findById(10L)).thenReturn(acc);
        when(transactionService.findById(1L)).thenReturn(existing);

        mockMvc.perform(get("/transactions/1/edit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(50.00));
    }

    @Test
    @DisplayName("GET /transactions/{id}/edit для чужой транзакции возвращает 403")
    void getTransactionEditForm_otherUsersTransaction_returnsForbidden() throws Exception {
        TransactionDto existing = new TransactionDto();
        existing.setId(1L);
        existing.setAccountId(10L);

        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(999L);

        when(transactionService.getTransactionById(1L)).thenReturn(existing);
        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(get("/transactions/1/edit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /transactions/{id}/update для своей транзакции обновляет и редиректит")
    void updateTransaction_ownTransaction_success() throws Exception {
        TransactionDto existing = new TransactionDto();
        existing.setId(1L);
        existing.setAccountId(10L);

        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(1L);

        when(transactionService.getTransactionById(1L)).thenReturn(existing);
        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/transactions/1/update")
                        .param("amount", "70.00")
                        .param("type", "EXPENSE")
                        .param("accountId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions"));

        verify(transactionService).updateTransaction(any(TransactionDto.class));
    }

    @Test
    @DisplayName("POST /transactions/{id}/update для чужой транзакции возвращает 403 и не обновляет")
    void updateTransaction_otherUsersTransaction_returnsForbidden() throws Exception {
        TransactionDto existing = new TransactionDto();
        existing.setId(1L);
        existing.setAccountId(10L);

        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(999L);

        when(transactionService.getTransactionById(1L)).thenReturn(existing);
        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/transactions/1/update")
                        .param("amount", "70.00")
                        .param("type", "EXPENSE")
                        .param("accountId", "10"))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).updateTransaction(any());
    }

    @Test
    @DisplayName("POST /transactions/{id}/delete для своей транзакции редиректит на страницу счёта")
    void deleteTransaction_ownTransaction_redirectsToAccountDetail() throws Exception {
        TransactionDto tx = new TransactionDto();
        tx.setId(1L);
        tx.setAccountId(10L);

        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(1L);

        when(transactionService.getTransactionById(1L)).thenReturn(tx);
        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/transactions/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/10"));

        verify(transactionService).deleteTransaction(1L);
    }

    @Test
    @DisplayName("POST /transactions/{id}/delete для чужой транзакции пишет ошибку и не удаляет")
    void deleteTransaction_otherUsersTransaction_setsFlashErrorAndRedirectsToTransactions() throws Exception {
        TransactionDto tx = new TransactionDto();
        tx.setId(1L);
        tx.setAccountId(10L);

        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(999L);

        when(transactionService.getTransactionById(1L)).thenReturn(tx);
        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/transactions/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions"))
                .andExpect(flash().attributeExists("error"));

        verify(transactionService, never()).deleteTransaction(any());
    }
}
