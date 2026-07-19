package com.finance.finance_tracker.service.Impl;


import com.finance.finance_tracker.DTO.ExchangeRateResponseDto;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.service.CurrencyApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyApiServiceImpl implements CurrencyApiService {

    private final WebClient.Builder webClientBuilder;


    @Value("${currencyapi.url}")
    private String apiUrl;

    @Value("${currencyapi.key}")
    private String apiKey;

    private static final String BASE_CURRENCY = "USD";


    @Cacheable(value = "exchangeRates", key = "#baseCurrency")
    public Map<String, Double> getExchangeRates(String baseCurrency) {
        log.info("Запрос актуальных курсов валют к внешнему API (база: {})", baseCurrency);
        String url = apiUrl + apiKey + "/latest/" + baseCurrency;

        try {
            ExchangeRateResponseDto response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new RuntimeException("Ошибка при вызове API: " + clientResponse.statusCode())))
                    .bodyToMono(ExchangeRateResponseDto.class)
                    .block(Duration.ofSeconds(3));

            if (response == null || response.getConversionRates() == null) {
                throw new RuntimeException("Не удалось получить курсы валют");
            }

            //save in cache
            saveLastSuccessfulRates(baseCurrency, response.getConversionRates());

            log.info("Получены курсы для {} валют", response.getConversionRates().size());
            return response.getConversionRates();

        } catch (Exception e) {
            log.error("Ошибка при получении курсов валют: {}", e.getMessage(), e);
            return getFallbackRates(baseCurrency);
        }
    }

    public BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        Map<String, Double> rates = getExchangeRates(fromCurrency);
        Double targetRate = rates.get(toCurrency.toUpperCase());

        if (targetRate == null) {
            throw new IllegalArgumentException("Курс для валюты " + toCurrency + " не найден");
        }

        return amount.multiply(BigDecimal.valueOf(targetRate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @CachePut(value = "fallbackExchangeRates", key = "#baseCurrency")
    public Map<String, Double> saveLastSuccessfulRates(String baseCurrency, Map<String, Double> rates) {
        return rates;
    }

    @Cacheable(value = "fallbackExchangeRates", key = "#baseCurrency", unless = "#result == null")
    public Map<String, Double> getFallbackRates(String baseCurrency) {

        log.warn("Используем статические курсы из enum для валюты {}", baseCurrency);
        return getStaticRates(baseCurrency);
    }

    private Map<String, Double> getStaticRates(String baseCurrency) {

        Map<String, Double> rates = new HashMap<>();
        for (Currency currency : Currency.values()) {
            rates.put(currency.name(), currency.getRateToRub().doubleValue());
        }
        return rates;
    }
}
