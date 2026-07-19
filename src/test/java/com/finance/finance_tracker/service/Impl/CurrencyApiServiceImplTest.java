package com.finance.finance_tracker.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit-тесты для {@link CurrencyApiServiceImpl}.
 *
 * ВАЖНО: {@code getExchangeRates(...)} сам по себе делает реальный вызов через
 * {@link WebClient} к внешнему API — тестировать это как чистый unit-тест
 * бессмысленно (пришлось бы мокать всю reactive-цепочку
 * WebClient -> RequestHeadersUriSpec -> RequestHeadersSpec -> ResponseSpec -> Mono).
 * Для проверки самого HTTP-вызова нужен отдельный интеграционный тест
 * с WireMock/MockWebServer.
 *
 * Здесь сервис оборачивается в Mockito.spy(), у которого подменяется только
 * getExchangeRates(...), а вся логика convertCurrency (короткие пути, округление,
 * обработка неизвестной валюты) тестируется по-настоящему.
 */
class CurrencyApiServiceImplTest {

    private CurrencyApiServiceImpl currencyApiService;

    @BeforeEach
    void setUp() {
        currencyApiService = spy(new CurrencyApiServiceImpl(WebClient.builder()));
    }

    @Test
    @DisplayName("для одинаковых валют возвращает исходную сумму без обращения к API")
    void convertCurrency_sameCurrency_returnsSameAmountWithoutApiCall() {
        BigDecimal result = currencyApiService.convertCurrency("USD", "USD", new BigDecimal("100.00"));

        assertThat(result).isEqualByComparingTo("100.00");
        verify(currencyApiService, never()).getExchangeRates(any());
    }

    @Test
    @DisplayName("для нулевой суммы возвращает 0 без обращения к API")
    void convertCurrency_zeroAmount_returnsZeroWithoutApiCall() {
        BigDecimal result = currencyApiService.convertCurrency("USD", "RUB", BigDecimal.ZERO);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(currencyApiService, never()).getExchangeRates(any());
    }

    @Test
    @DisplayName("для отрицательной суммы возвращает 0")
    void convertCurrency_negativeAmount_returnsZero() {
        BigDecimal result = currencyApiService.convertCurrency("USD", "RUB", new BigDecimal("-5.00"));

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("для null суммы возвращает 0")
    void convertCurrency_nullAmount_returnsZero() {
        BigDecimal result = currencyApiService.convertCurrency("USD", "RUB", null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("применяет курс из getExchangeRates и округляет до 2 знаков")
    void convertCurrency_differentCurrency_appliesRateAndRounds() {
        doReturn(Map.of("RUB", 90.5)).when(currencyApiService).getExchangeRates("USD");

        BigDecimal result = currencyApiService.convertCurrency("USD", "RUB", new BigDecimal("10.00"));

        assertThat(result).isEqualByComparingTo("905.00");
    }

    @Test
    @DisplayName("бросает IllegalArgumentException, если курс для целевой валюты не найден")
    void convertCurrency_unknownTargetCurrency_throws() {
        doReturn(Map.of("EUR", 1.0)).when(currencyApiService).getExchangeRates("USD");

        assertThrows(IllegalArgumentException.class,
                () -> currencyApiService.convertCurrency("USD", "RUB", BigDecimal.TEN));
    }

    @Test
    @DisplayName("сравнение исходной и целевой валюты регистронезависимо")
    void convertCurrency_sameCurrencyDifferentCase_returnsSameAmount() {
        BigDecimal result = currencyApiService.convertCurrency("usd", "USD", new BigDecimal("50.00"));

        assertThat(result).isEqualByComparingTo("50.00");
        verify(currencyApiService, never()).getExchangeRates(any());
    }
}
