package com.finance.finance_tracker.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Получение актуальных курсов валют из внешнего сервиса
 * (<a href="https://www.exchangerate-api.com/">exchangerate-api.com</a>,
 * см. {@code currencyapi.url}/{@code currencyapi.key} в {@code application.yaml})
 * и конвертация сумм между валютами.
 */
public interface CurrencyApiService {

    /**
     * Возвращает курсы всех доступных валют относительно указанной базовой.
     * Результат кэшируется (см. {@code @Cacheable} в реализации) по ключу
     * {@code baseCurrency} — на момент написания кэш не имеет TTL/инвалидации,
     * поэтому курсы могут "протухнуть" без перезапуска приложения.
     *
     * @param baseCurrency код базовой валюты, ISO 4217 (например, {@code "USD"})
     * @return карта {@code код валюты -> курс относительно baseCurrency}
     * @throws RuntimeException если внешний сервис недоступен или вернул ошибку
     */
    Map<String, Double> getExchangeRates(String baseCurrency);

    /**
     * Конвертирует сумму из одной валюты в другую. Для одинаковых кодов
     * валют (без учёта регистра) и для нулевой/отрицательной/{@code null}
     * суммы возвращает результат без обращения к {@link #getExchangeRates}.
     *
     * @param fromCurrency код исходной валюты, ISO 4217
     * @param toCurrency   код целевой валюты, ISO 4217
     * @param amount       сумма к конвертации
     * @return сконвертированная сумма, округлённая до 2 знаков после запятой
     * @throws IllegalArgumentException если курс для {@code toCurrency} не найден
     */
    BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount);
}
