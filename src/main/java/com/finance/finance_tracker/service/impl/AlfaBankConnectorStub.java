// src/main/java/com/finance/finance_tracker/service/impl/AlfaBankConnectorStub.java
package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import com.finance.finance_tracker.service.BankConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Заглушка на время, пока не готовы реальные OAuth2-креды Alfa
 * (ALFA_CLIENT_ID/ALFA_CLIENT_SECRET + подтверждённые хосты
 * авторизации). Регистрируется под тем же именем бина, что и реальный
 * AlfaBankConnector ("alfaBankConnector") — BankImportServiceImpl не
 * меняется независимо от режима, Spring Boot включает ровно один из
 * двух бинов по значению alfa.mode.
 *
 * externalId зафиксированы (не генерируются заново на каждый вызов) —
 * значит повторный запуск синхронизации корректно найдёт "уже
 * импортировано" через существующую дедупликацию в
 * BankImportServiceImpl и не создаст дублей, ровно как было бы с
 * реальным банком.
 */
@Slf4j
@Component("alfaBankConnector")
@ConditionalOnProperty(prefix = "alfa", name = "mode", havingValue = "stub", matchIfMissing = true)
public class AlfaBankConnectorStub implements BankConnector {

    @Override
    public String bankCode() {
        return "ALFA";
    }

    @Override
    public BankStatementResult fetchTransactions(String accountNumber, LocalDateTime from, LocalDateTime to, String principalName) {
        log.warn("AlfaBankConnectorStub активен (alfa.mode=stub) — возвращаю тестовые данные без обращения к Alfa API. " +
                "accountNumber={}, principal={}", accountNumber, principalName);

        List<BankTransactionDto> transactions = List.of(
                new BankTransactionDto(
                        "alfa-stub-op-1",
                        accountNumber,
                        new BigDecimal("75000.00"),
                        "RUB",
                        "Зарплата (тестовые данные Alfa stub)",
                        LocalDateTime.now().minusDays(5)),
                new BankTransactionDto(
                        "alfa-stub-op-2",
                        accountNumber,
                        new BigDecimal("-1200.50"),
                        "RUB",
                        "Супермаркет (тестовые данные Alfa stub)",
                        LocalDateTime.now().minusDays(3)),
                new BankTransactionDto(
                        "alfa-stub-op-3",
                        accountNumber,
                        new BigDecimal("-499.00"),
                        "RUB",
                        "Подписка (тестовые данные Alfa stub)",
                        LocalDateTime.now().minusDays(1))
        );

        return new BankStatementResult(transactions, null);
    }
}