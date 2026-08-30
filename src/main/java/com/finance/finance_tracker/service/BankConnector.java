package com.finance.finance_tracker.service;

import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;

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
     * @param principalName email текущего пользователя приложения
     *                       (Authentication.getName()) — нужен коннекторам
     *                       с per-пользовательской OAuth2-авторизацией
     *                       (Alfa). Статическим коннекторам (T-Bank) не
     *                       нужен, можно игнорировать.
     */
    BankStatementResult fetchTransactions(String accountNumber, LocalDateTime from, LocalDateTime to, String principalName);
}