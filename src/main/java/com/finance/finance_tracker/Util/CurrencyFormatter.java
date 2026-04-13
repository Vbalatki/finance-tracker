package com.finance.finance_tracker.Util;

import com.finance.finance_tracker.entity.enums.Currency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class CurrencyFormatter {

    /**
     * Форматирует сумму с символом валюты
     */
    public String formatAmount(BigDecimal amount, Currency currency) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        if (currency == null) {
            currency = Currency.RUB;
        }

        // Форматируем число
        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
        String formattedAmount = df.format(amount);

        // Добавляем символ валюты
        switch (currency) {
            case USD:
                return "$" + formattedAmount;
            case EUR:
                return "€" + formattedAmount;
            case GBP:
                return "£" + formattedAmount;
            case KZT:
                return formattedAmount + " ₸";
            case RUB:
            default:
                return formattedAmount + " ₽";
        }
    }

    /**
     * Форматирует сумму в рублях (для общего баланса)
     */
    public String formatAmountInRub(BigDecimal amount) {
        return formatAmount(amount, Currency.RUB);
    }

    /**
     * Возвращает символ валюты
     */
    public String getSymbol(Currency currency) {
        if (currency == null) {
            return "₽";
        }

        switch (currency) {
            case USD: return "$";
            case EUR: return "€";
            case GBP: return "£";
            case KZT: return "₸";
            case RUB: default: return "₽";
        }
    }

    /**
     * Возвращает символ и код валюты
     */
    public String getSymbolWithCode(Currency currency) {
        String symbol = getSymbol(currency);
        return symbol + " " + (currency != null ? currency.name() : "RUB");
    }
}