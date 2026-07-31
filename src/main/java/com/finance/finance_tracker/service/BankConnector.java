package com.finance.finance_tracker.service;

import com.finance.finance_tracker.exception.BankIntegrationException;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import com.finance.finance_tracker.dto.bank.BankStatementResult;

import java.time.LocalDateTime;

/**
 * Общий контракт для чтения транзакций из внешнего банка.
 * Каждый банк (T-Bank, в перспективе — другие) реализует этот интерфейс
 * своим коннектором, инкапсулируя собственный формат ответа и способ
 * авторизации. Вызывающий код работает только с этим интерфейсом и
 * с нормализованным {@link BankTransactionDto} — про конкретный банк
 * ничего не знает.
 */
public interface BankConnector {

    /**
     * Код банка для логирования/идентификации коннектора в списке
     * подключённых интеграций (например, "TBANK").
     */
    String bankCode();

    /**
     * Возвращает транзакции по счёту за период. Только чтение —
     * никаких операций записи/перевода этот метод не предполагает.
     *
     * @param accountNumber номер счёта в банке (не id вашего Account!)
     * @param from          начало периода
     * @param to            конец периода
     * @return список транзакций в нормализованном формате
     * @throws BankIntegrationException при ошибке обращения к банку
     */
    BankStatementResult fetchTransactions(String accountNumber, LocalDateTime from, LocalDateTime to);
}