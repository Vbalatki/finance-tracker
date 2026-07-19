package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.Util.CurrencyFormatter;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.exception.InsufficientFundsException;
import com.finance.finance_tracker.handler.GlobalExceptionHandler;
import com.finance.finance_tracker.service.AccountService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link AccountController}. MockMvc собирается через standaloneSetup —
 * это позволяет проверять view-имена/redirect'ы/flash-атрибуты без поднятия
 * полного Spring-контекста и без реального рендеринга Thymeleaf-шаблонов.
 * Аутентификация имитируется прямой записью в SecurityContextHolder
 * (её читают и @AuthenticationPrincipal, и SecurityUtil.getCurrentUserId()).
 */
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CurrencyFormatter currencyFormatter = new CurrencyFormatter();
        AccountController controller =
                new AccountController(userService, accountService, transactionService, currencyFormatter);

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
    @DisplayName("GET /accounts без аутентификации редиректит на /login")
    void accountsPage_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/accounts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /accounts возвращает список счетов текущего пользователя")
    void accountsPage_authenticated_returnsAccountsList() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setName("Счет");
        acc.setBalance(new BigDecimal("100.00"));
        acc.setCurrency(Currency.RUB);
        acc.setUserId(1L);

        when(accountService.getUserAccounts(1L)).thenReturn(List.of(acc));
        when(userService.getUserTotalBalanceInRub(List.of(acc))).thenReturn(new BigDecimal("100.00"));

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/list"))
                .andExpect(model().attribute("accounts", List.of(acc)));
    }

    @Test
    @DisplayName("POST /accounts с валидными данными создаёт счёт и редиректит")
    void createAccount_valid_redirectsToAccounts() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("name", "Новый счет")
                        .param("balance", "0")
                        .param("currency", "RUB")
                        .param("userId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"));

        verify(accountService).saveAccount(any(AccountDto.class));
    }

    @Test
    @DisplayName("POST /accounts без имени возвращает форму с ошибками валидации")
    void createAccount_blankName_returnsCreateView() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("name", "")
                        .param("balance", "0")
                        .param("currency", "RUB")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/create"));

        verify(accountService, never()).saveAccount(any());
    }

    @Test
    @DisplayName("GET /accounts/{id} для своего счёта возвращает страницу деталей")
    void accountDetail_ownAccount_returnsDetailView() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(1L);
        acc.setBalance(BigDecimal.TEN);
        acc.setCurrency(Currency.RUB);

        when(accountService.findById(10L)).thenReturn(acc);
        when(transactionService.findByAccountId(10L)).thenReturn(List.of());

        mockMvc.perform(get("/accounts/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/detail"));
    }

    @Test
    @DisplayName("GET /accounts/{id} для чужого счёта возвращает 403")
    void accountDetail_otherUsersAccount_returnsForbidden() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(20L);
        acc.setUserId(999L);

        when(accountService.findById(20L)).thenReturn(acc);

        mockMvc.perform(get("/accounts/20"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/{id}/deposit пополняет свой счёт и редиректит с сообщением об успехе")
    void deposit_ownAccount_success() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(1L);

        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/accounts/10/deposit").param("amount", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/10"))
                .andExpect(flash().attributeExists("success"));

        verify(accountService).deposit(10L, new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("POST /accounts/{id}/deposit для чужого счёта не пополняет и пишет ошибку во flash")
    void deposit_otherUsersAccount_setsFlashErrorAndDoesNotDeposit() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(999L);

        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/accounts/10/deposit").param("amount", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        verify(accountService, never()).deposit(any(), any());
    }

    @Test
    @DisplayName("POST /accounts/{id}/withdraw при недостатке средств пишет ошибку во flash, не бросая 500")
    void withdraw_insufficientFunds_setsFlashError() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(1L);

        when(accountService.findById(10L)).thenReturn(acc);
        when(accountService.withdraw(10L, new BigDecimal("100.00")))
                .thenThrow(new InsufficientFundsException("Недостаточно средств"));

        mockMvc.perform(post("/accounts/10/withdraw").param("amount", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/10"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /accounts/{id}/delete удаляет свой счёт и редиректит на список")
    void deleteAccount_ownAccount_success() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(1L);
        acc.setName("Счет");

        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/accounts/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"))
                .andExpect(flash().attributeExists("success"));

        verify(accountService).deleteAccount(10L);
    }

    @Test
    @DisplayName("POST /accounts/{id}/delete для чужого счёта не удаляет и пишет ошибку")
    void deleteAccount_otherUsersAccount_setsFlashErrorAndDoesNotDelete() throws Exception {
        AccountDto acc = new AccountDto();
        acc.setId(10L);
        acc.setUserId(999L);

        when(accountService.findById(10L)).thenReturn(acc);

        mockMvc.perform(post("/accounts/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        verify(accountService, never()).deleteAccount(any());
    }
}
