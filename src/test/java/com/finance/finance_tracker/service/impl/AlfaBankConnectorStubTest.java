package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AlfaBankConnectorStubTest {

    private final AlfaBankConnectorStub connector = new AlfaBankConnectorStub();

    @Test
    @DisplayName("bankCode возвращает ALFA — используется resolveConnector в BankImportServiceImpl")
    void bankCode_isAlfa() {
        assertThat(connector.bankCode()).isEqualTo("ALFA");
    }

    @Test
    @DisplayName("возвращает непустой список тестовых транзакций")
    void fetchTransactions_returnsNonEmptyStubData() {
        BankStatementResult result = connector.fetchTransactions(
                "40702810000000000001", LocalDateTime.now().minusDays(30), LocalDateTime.now(), "user@example.com");

        assertThat(result.transactions()).isNotEmpty();
    }

    @Test
    @DisplayName("externalId стабильны между двумя вызовами — критично для дедупликации в BankImportServiceImpl")
    void fetchTransactions_calledTwice_returnsSameExternalIds() {
        BankStatementResult first = connector.fetchTransactions(
                "40702810000000000001", LocalDateTime.now().minusDays(30), LocalDateTime.now(), "user@example.com");
        BankStatementResult second = connector.fetchTransactions(
                "40702810000000000001", LocalDateTime.now().minusDays(30), LocalDateTime.now(), "user@example.com");

        Set<String> firstIds = first.transactions().stream()
                .map(BankTransactionDto::externalId)
                .collect(Collectors.toSet());
        Set<String> secondIds = second.transactions().stream()
                .map(BankTransactionDto::externalId)
                .collect(Collectors.toSet());

        // если бы id генерировались заново на каждый вызов (например, UUID.randomUUID()) —
        // повторная синхронизация считала бы одни и те же тестовые операции новыми
        // каждый раз, и дедупликация по (bankCode, externalId) не сработала бы
        assertThat(secondIds).isEqualTo(firstIds);
    }

    @Test
    @DisplayName("externalId уникальны внутри одного ответа — не создают ложных дублей уже на первом вызове")
    void fetchTransactions_operationsHaveUniqueExternalIdsWithinOneCall() {
        BankStatementResult result = connector.fetchTransactions(
                "40702810000000000001", LocalDateTime.now().minusDays(30), LocalDateTime.now(), "user@example.com");

        List<String> ids = result.transactions().stream()
                .map(BankTransactionDto::externalId)
                .collect(Collectors.toList());

        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("endingBalance всегда null — заглушка не должна перезаписывать реальный баланс счёта")
    void fetchTransactions_endingBalanceIsNull() {
        BankStatementResult result = connector.fetchTransactions(
                "40702810000000000001", LocalDateTime.now().minusDays(30), LocalDateTime.now(), "user@example.com");

        assertThat(result.endingBalance()).isNull();
    }
}