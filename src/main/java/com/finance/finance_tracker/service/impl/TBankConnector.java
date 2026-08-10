package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import com.finance.finance_tracker.dto.bank.TBankOperationDto;
import com.finance.finance_tracker.dto.bank.TBankStatementResponseDto;
import com.finance.finance_tracker.exception.BankIntegrationException;
import com.finance.finance_tracker.service.BankConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TBankConnector implements BankConnector {

    private final WebClient tbankWebClient;

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    private static final Map<String, String> DIGITAL_TO_ALPHA_CURRENCY = Map.of(
            "643", "RUB",
            "840", "USD",
            "978", "EUR",
            "826", "GBP",
            "398", "KZT",
            "392", "JPY"
    );

    public TBankConnector(@Qualifier("tbankWebClient") WebClient tbankWebClient) {
        this.tbankWebClient = tbankWebClient;
    }

    @Override
    public String bankCode() {
        return "TBANK";
    }

    @Override
    public BankStatementResult fetchTransactions(String accountNumber, LocalDateTime from, LocalDateTime to) {
        log.info("Запрос выписки Т-Банк: accountNumber={}, период {} - {}", accountNumber, from, to);

        try {
            TBankStatementResponseDto response = tbankWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/openapi/sandbox/api/v1/statement")
                            .queryParam("accountNumber", accountNumber)
                            .queryParam("from", from.format(ISO_FORMAT) + "Z")
                            .queryParam("to", to.format(ISO_FORMAT) + "Z")
                            .queryParam("withBalances", true)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new BankIntegrationException(
                                            "Т-Банк вернул ошибку " + clientResponse.statusCode() + ": " + body)))
                    .bodyToMono(TBankStatementResponseDto.class)
                    .block();

            if (response == null || response.getOperations() == null) {
                log.warn("Пустой ответ от Т-Банк для счёта {}", accountNumber);
                return new BankStatementResult(Collections.emptyList(), null);
            }

            if (response.getNextCursor() != null && !response.getNextCursor().isBlank()) {
                log.warn("Ответ Т-Банк для счёта {} содержит nextCursor — часть операций за период не получена (пагинация не реализована)",
                        accountNumber);
            }

            List<BankTransactionDto> transactions = response.getOperations().stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());

            BigDecimal endingBalance = response.getBalances() != null
                    ? response.getBalances().getBalanceEnd()
                    : null;

            log.info("Получено {} операций по счёту {}", transactions.size(), accountNumber);
            return new BankStatementResult(transactions, endingBalance);

        } catch (BankIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обращении к Т-Банк API: {}", e.getMessage(), e);
            throw new BankIntegrationException("Не удалось получить выписку из Т-Банк", e);
        }
    }

    private BankTransactionDto toDomain(TBankOperationDto raw) {
        boolean isCredit = "Credit".equalsIgnoreCase(raw.getTypeOfOperation());
        BigDecimal signedAmount = isCredit
                ? raw.getAccountAmount()
                : raw.getAccountAmount().negate();

        String description = (raw.getDescription() != null && !raw.getDescription().isBlank())
                ? raw.getDescription()
                : raw.getPayPurpose();

        return new BankTransactionDto(
                raw.getOperationId(),
                raw.getAccountNumber(),
                signedAmount,
                toAlphaCurrency(raw.getAccountCurrencyDigitalCode()),
                description,
                LocalDateTime.parse(raw.getOperationDate(), ISO_FORMAT)
        );
    }

    private String toAlphaCurrency(String digitalCode) {
        String alpha = DIGITAL_TO_ALPHA_CURRENCY.get(digitalCode);
        if (alpha == null) {
            log.warn("Неизвестный цифровой код валюты от Т-Банк: {}, использую RUB по умолчанию", digitalCode);
            return "RUB";
        }
        return alpha;
    }
}