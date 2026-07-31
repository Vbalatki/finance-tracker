package com.finance.finance_tracker.dto.bank;

import java.math.BigDecimal;
import java.util.List;

/**
 * Результат чтения выписки за период: список операций + баланс на конец
 * периода, если банк его вернул. Раньше BankConnector возвращал голый
 * List<BankTransactionDto> — баланс было негде передать наверх, теперь
 * его несёт этот wrapper.
 */
public record BankStatementResult(
        List<BankTransactionDto> transactions,
        BigDecimal endingBalance
) {
}