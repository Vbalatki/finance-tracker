package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.service.BankImportService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AlfaBankIntegrationStubControllerTest {

    @Mock
    private BankImportService bankImportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AlfaBankIntegrationStubController controller = new AlfaBankIntegrationStubController(bankImportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
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
    @DisplayName("GET /connect без аутентификации редиректит на /login")
    void connect_notAuthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/bank-integration/alfa/connect"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /connect с аутентификацией сразу возвращает форму — без редиректа на банк")
    void connect_authenticated_returnsLinkFormDirectly() throws Exception {
        authenticateAsUserId(1L, "user@example.com");

        mockMvc.perform(get("/bank-integration/alfa/connect"))
                .andExpect(status().isOk())
                .andExpect(view().name("bank-integration/alfa-link"));
    }

    @Test
    @DisplayName("POST /link с валидными данными привязывает счёт и редиректит на страницу счёта")
    void link_valid_redirectsToAccountDetail() throws Exception {
        authenticateAsUserId(1L, "user@example.com");
        when(bankImportService.linkAccount(eq(1L), eq("ALFA"), anyString(), anyString(), any()))
                .thenReturn(42L);

        mockMvc.perform(post("/bank-integration/alfa/link")
                        .param("externalAccountNumber", "40702810000000000001")
                        .param("accountName", "Альфа расчётный")
                        .param("currency", "RUB"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/42"))
                .andExpect(flash().attributeExists("success"));

        verify(bankImportService).linkAccount(1L, "ALFA", "40702810000000000001", "Альфа расчётный",
                com.finance.finance_tracker.entity.enums.Currency.RUB);
    }

    @Test
    @DisplayName("POST /link при ошибке сервиса (например, дубликат) возвращает форму с сообщением об ошибке")
    void link_serviceThrows_returnsFormWithError() throws Exception {
        authenticateAsUserId(1L, "user@example.com");
        when(bankImportService.linkAccount(eq(1L), eq("ALFA"), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Счёт уже привязан"));

        mockMvc.perform(post("/bank-integration/alfa/link")
                        .param("externalAccountNumber", "40702810000000000001")
                        .param("accountName", "Альфа расчётный")
                        .param("currency", "RUB"))
                .andExpect(status().isOk())
                .andExpect(view().name("bank-integration/alfa-link"));
    }
}