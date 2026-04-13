package com.finance.finance_tracker.entity.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Currency {
    RUB("Рубль", BigDecimal.ONE),  // Базовая валюта
    USD("Доллар США", new BigDecimal("90.50")),  // 1 USD = 90.50 RUB
    EUR("Евро", new BigDecimal("100.20")),       // 1 EUR = 100.20 RUB
    KZT("Тенге", new BigDecimal("0.20")),        // 1 KZT = 0.20 RUB
    GBP("Фунт", new BigDecimal("115.30")),
    JPY("Иена", new BigDecimal("0.62"));

    private final String displayName;
    private final BigDecimal rateToRub; // Курс к рублю

    Currency(String displayName, BigDecimal rateToRub) {
        this.displayName = displayName;
        this.rateToRub = rateToRub;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getRateToRub() {
        return rateToRub;
    }

    // Конвертация суммы в рубли
    public BigDecimal convertToRub(BigDecimal amount) {
        return amount.multiply(rateToRub);
    }

    // Конвертация из рублей в эту валюту
    public BigDecimal convertFromRub(BigDecimal rubAmount) {
        return rubAmount.divide(rateToRub, 2, RoundingMode.HALF_UP);
    }

    // Конвертация из одной валюты в другую
    public static BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (from == to) return amount;
        BigDecimal inRub = from.convertToRub(amount);
        return to.convertFromRub(inRub);
    }
}