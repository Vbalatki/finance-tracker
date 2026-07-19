package com.finance.finance_tracker.Util;

import com.finance.finance_tracker.entity.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link CurrencyFormatter}. Чистая логика форматирования,
 * зависимостей нет — моки не нужны.
 */
class CurrencyFormatterTest {

    private final CurrencyFormatter formatter = new CurrencyFormatter();

    @Test
    @DisplayName("форматирует рубли с символом ₽")
    void formatAmount_rub() {
        assertThat(formatter.formatAmount(new BigDecimal("1234.5"), Currency.RUB)).isEqualTo("1,234.50 ₽");
    }

    @Test
    @DisplayName("форматирует доллары с символом $ перед суммой")
    void formatAmount_usd() {
        assertThat(formatter.formatAmount(new BigDecimal("1234.5"), Currency.USD)).isEqualTo("$1,234.50");
    }

    @Test
    @DisplayName("форматирует евро с символом € перед суммой")
    void formatAmount_eur() {
        assertThat(formatter.formatAmount(new BigDecimal("100"), Currency.EUR)).isEqualTo("€100.00");
    }

    @Test
    @DisplayName("форматирует фунты с символом £ перед суммой")
    void formatAmount_gbp() {
        assertThat(formatter.formatAmount(new BigDecimal("100"), Currency.GBP)).isEqualTo("£100.00");
    }

    @Test
    @DisplayName("форматирует тенге с символом ₸ после суммы")
    void formatAmount_kzt() {
        assertThat(formatter.formatAmount(new BigDecimal("100"), Currency.KZT)).isEqualTo("100.00 ₸");
    }

    @Test
    @DisplayName("null-сумма считается нулевой")
    void formatAmount_nullAmount_treatedAsZero() {
        assertThat(formatter.formatAmount(null, Currency.RUB)).isEqualTo("0.00 ₽");
    }

    @Test
    @DisplayName("null-валюта по умолчанию считается рублём")
    void formatAmount_nullCurrency_defaultsToRub() {
        assertThat(formatter.formatAmount(BigDecimal.TEN, null)).isEqualTo("10.00 ₽");
    }

    @Test
    @DisplayName("formatAmountInRub всегда форматирует как рубли")
    void formatAmountInRub_success() {
        assertThat(formatter.formatAmountInRub(new BigDecimal("50"))).isEqualTo("50.00 ₽");
    }

    @Test
    @DisplayName("getSymbol возвращает корректный символ для каждой валюты")
    void getSymbol_returnsCorrectSymbols() {
        assertThat(formatter.getSymbol(Currency.RUB)).isEqualTo("₽");
        assertThat(formatter.getSymbol(Currency.USD)).isEqualTo("$");
        assertThat(formatter.getSymbol(Currency.EUR)).isEqualTo("€");
        assertThat(formatter.getSymbol(Currency.GBP)).isEqualTo("£");
        assertThat(formatter.getSymbol(Currency.KZT)).isEqualTo("₸");
        assertThat(formatter.getSymbol(null)).isEqualTo("₽");
    }

    @Test
    @DisplayName("getSymbolWithCode возвращает символ и код валюты")
    void getSymbolWithCode_success() {
        assertThat(formatter.getSymbolWithCode(Currency.USD)).isEqualTo("$ USD");
        assertThat(formatter.getSymbolWithCode(null)).isEqualTo("₽ RUB");
    }
}
