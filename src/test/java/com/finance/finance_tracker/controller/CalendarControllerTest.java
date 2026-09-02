package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.CalendarMonthDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.service.SpendingCalendarService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link CalendarController} через standalone MockMvc.
 *
 * <p>Ключевой сценарий — {@code invalidMonth_redirectsInsteadOfThrowing}:
 * {@code YearMonth.of(year, month)} бросает {@link java.time.DateTimeException}
 * для {@code month} вне 1-12. Без обработки в контроллере это исключение
 * долетало бы до {@code GlobalExceptionHandler.handleGenericException}
 * ({@code @RestControllerAdvice} ловит любые контроллеры, не только REST) —
 * пользователь вместо HTML-страницы календаря получал бы голый JSON с
 * ошибкой. Тест доказывает, что вместо этого происходит редирект на
 * {@code /calendar} без параметров, без исключения наружу.
 */
@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private SpendingCalendarService spendingCalendarService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CalendarController controller = new CalendarController(spendingCalendarService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

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

    private CalendarMonthDto dummyMonth(YearMonth month) {
        return new CalendarMonthDto(month, List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /calendar без параметров показывает текущий месяц")
    void calendar_noParams_showsCurrentMonth() throws Exception {
        YearMonth now = YearMonth.now();
        when(spendingCalendarService.getMonth(eq(1L), eq(now))).thenReturn(dummyMonth(now));

        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/index"))
                .andExpect(model().attribute("prevMonth", now.minusMonths(1)))
                .andExpect(model().attribute("nextMonth", now.plusMonths(1)));
    }

    @Test
    @DisplayName("GET /calendar с валидными year/month показывает указанный месяц")
    void calendar_validYearMonth_showsThatMonth() throws Exception {
        YearMonth target = YearMonth.of(2026, 6);
        when(spendingCalendarService.getMonth(eq(1L), eq(target))).thenReturn(dummyMonth(target));

        mockMvc.perform(get("/calendar").param("year", "2026").param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/index"))
                .andExpect(model().attribute("prevMonth", YearMonth.of(2026, 5)))
                .andExpect(model().attribute("nextMonth", YearMonth.of(2026, 7)));

        verify(spendingCalendarService).getMonth(1L, target);
    }

    @Test
    @DisplayName("GET /calendar?month=13 — DateTimeException не пробрасывается, редирект на /calendar вместо JSON-ошибки")
    void calendar_month13_redirectsInsteadOfThrowing() throws Exception {
        mockMvc.perform(get("/calendar").param("year", "2026").param("month", "13"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        // сервис вообще не должен был вызваться с невалидными параметрами
        verify(spendingCalendarService, never()).getMonth(any(), any());
    }

    @Test
    @DisplayName("GET /calendar?month=0 — тоже вне диапазона 1-12, тот же редирект")
    void calendar_month0_redirectsInsteadOfThrowing() throws Exception {
        mockMvc.perform(get("/calendar").param("year", "2026").param("month", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        verify(spendingCalendarService, never()).getMonth(any(), any());
    }

    @Test
    @DisplayName("GET /calendar только с year (без month) — валидация не запускается, показывается текущий месяц")
    void calendar_onlyYearProvided_fallsBackToCurrentMonth() throws Exception {
        YearMonth now = YearMonth.now();
        when(spendingCalendarService.getMonth(eq(1L), eq(now))).thenReturn(dummyMonth(now));

        mockMvc.perform(get("/calendar").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar/index"));

        verify(spendingCalendarService).getMonth(1L, now);
    }
}
