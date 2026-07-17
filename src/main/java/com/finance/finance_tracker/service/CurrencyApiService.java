package com.finance.finance_tracker.service;

import java.math.BigDecimal;
import java.util.Map;

public interface CurrencyApiService {
    Map<String, Double> getExchangeRates(String baseCurrency);
    BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount);

}
