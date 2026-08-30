package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.bank.AlfaOperationDto;
import com.finance.finance_tracker.dto.bank.AlfaStatementResponseDto;
import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import com.finance.finance_tracker.exception.BankIntegrationException;
import com.finance.finance_tracker.service.BankConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * В отличие от TBankConnector, токен не статический — на каждый вызов
 * запрашивается (и при необходимости автообновляется по refresh_token)
 * через OAuth2AuthorizedClientManager для конкретного principalName.
 * Если пользователь ещё не привязал Alfa через
 * AlfaBankIntegrationController.connect() (нет сохранённого authorized
 * client) — authorize() вернёт null, бросаем понятную ошибку вместо NPE.
 *
 * Бин называется "alfaBankConnector" (имя по умолчанию от Spring —
 * имя класса с маленькой буквы) — используется в @Qualifier при
 * инъекции в BankImportServiceImpl, там же второй BankConnector-бин
 * "tBankConnector".
 */
@Slf4j
@Component("alfaBankConnector")
@ConditionalOnProperty(prefix = "alfa", name = "mode", havingValue = "oauth2")
public class AlfaBankConnector implements BankConnector {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final WebClient alfaWebClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public AlfaBankConnector(WebClient alfaWebClient, OAuth2AuthorizedClientManager authorizedClientManager) {
        this.alfaWebClient = alfaWebClient;
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public String bankCode() {
        return "ALFA";
    }

    @Override
    public BankStatementResult fetchTransactions(String accountNumber, LocalDateTime from, LocalDateTime to, String principalName) {
        String accessToken = resolveAccessToken(principalName);

        log.info("Запрос выписки Alfa: accountNumber={}, principal={}, период {} - {}",
                accountNumber, principalName, from, to);

        try {
            AlfaStatementResponseDto response = alfaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/statement/transactions")
                            .queryParam("accountNumber", accountNumber)
                            .queryParam("statementDate", from.toLocalDate().format(DATE_FORMAT))
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new BankIntegrationException(
                                            "Alfa API вернул ошибку " + clientResponse.statusCode() + ": " + body)))
                    .bodyToMono(AlfaStatementResponseDto.class)
                    .block();

            if (response == null || response.getOperations() == null) {
                log.warn("Пустой ответ от Alfa API для счёта {}", accountNumber);
                return new BankStatementResult(Collections.emptyList(), null);
            }

            if (response.getNextPage() != null && !response.getNextPage().isBlank()) {
                log.warn("Ответ Alfa для счёта {} содержит nextPage — пагинация не реализована, " +
                        "часть операций за период не получена", accountNumber);
            }

            List<BankTransactionDto> transactions = response.getOperations().stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());

            log.info("Получено {} операций по счёту {}", transactions.size(), accountNumber);
            // Alfa statement/transactions не возвращает остаток на конец периода в этом эндпоинте
            // (в отличие от T-Bank) — endingBalance сознательно null, баланс счёта не обновляется отсюда.
            return new BankStatementResult(transactions, null);

        } catch (BankIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обращении к Alfa API: {}", e.getMessage(), e);
            throw new BankIntegrationException("Не удалось получить выписку из Альфа-Банка", e);
        }
    }

    private String resolveAccessToken(String principalName) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("alfabank")
                .principal(principalName)
                .build();

        OAuth2AuthorizedClient authorizedClient;
        try {
            authorizedClient = authorizedClientManager.authorize(authorizeRequest);
        } catch (OAuth2AuthorizationException e) {
            throw new BankIntegrationException(
                    "Не удалось обновить доступ к Alfa API — возможно, нужно привязать счёт заново", e);
        }

        if (authorizedClient == null) {
            throw new BankIntegrationException(
                    "Alfa-аккаунт не привязан для пользователя " + principalName +
                    " — сначала пройдите привязку через /bank-integration/alfa/connect");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    private BankTransactionDto toDomain(AlfaOperationDto raw) {
        boolean isIncome = "INCOME".equalsIgnoreCase(raw.getDirection());
        BigDecimal rawAmount = raw.getAmount() != null && raw.getAmount().getValue() != null
                ? raw.getAmount().getValue()
                : BigDecimal.ZERO;
        BigDecimal signedAmount = isIncome ? rawAmount : rawAmount.negate();

        return new BankTransactionDto(
                raw.getId(),
                null, // accountNumber не дублируется в самой операции у Alfa — не критично, коннектор уже знает счёт по контексту запроса
                signedAmount,
                raw.getAmount() != null ? raw.getAmount().getCurrency() : "RUB",
                raw.getTitle(),
                LocalDateTime.parse(raw.getDateTime(), DateTimeFormatter.ISO_DATE_TIME)
        );
    }
}
