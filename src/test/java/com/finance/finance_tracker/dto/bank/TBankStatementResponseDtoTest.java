package com.finance.finance_tracker.dto.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TBankStatementResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("десериализует полный ответ песочницы, включая вложенные operations и balances")
    void deserialize_fullResponse_mapsAllFields() throws Exception {
        String json = """
                {
                  "operations": [
                    {
                      "operationId": "64be58f9-c7fc-0027-96ba-763ec56a2317",
                      "accountNumber": "40702810110011000000",
                      "typeOfOperation": "Credit",
                      "description": "Зарплата",
                      "payPurpose": "Перевод по договору",
                      "accountAmount": 50000.00,
                      "accountCurrencyDigitalCode": "643",
                      "operationDate": "2026-01-15T10:30:00"
                    }
                  ],
                  "balances": { "balanceEnd": 125000.50 },
                  "nextCursor": null,
                  "unexpectedNewField": "должно быть проигнорировано"
                }
                """;

        TBankStatementResponseDto dto = objectMapper.readValue(json, TBankStatementResponseDto.class);

        TBankOperationDto op = dto.getOperations().get(0);
        assertThat(op.getOperationId()).isEqualTo("64be58f9-c7fc-0027-96ba-763ec56a2317");
        assertThat(op.getAccountAmount()).isEqualByComparingTo("50000.00");
        assertThat(dto.getBalances().getBalanceEnd()).isEqualByComparingTo("125000.50");
        assertThat(dto.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("description=\"\" десериализуется как пустая строка, не null — от этого зависит fallback на payPurpose в TBankConnector.toDomain")
    void operationDto_blankDescription_deserializesAsEmptyNotNull() throws Exception {
        String json = """
                {
                  "operationId": "op-1",
                  "accountNumber": "40702810110011000000",
                  "typeOfOperation": "Debit",
                  "description": "",
                  "payPurpose": "Оплата подписки",
                  "accountAmount": 799.00,
                  "accountCurrencyDigitalCode": "643",
                  "operationDate": "2026-01-01T00:00:00"
                }
                """;

        TBankOperationDto op = objectMapper.readValue(json, TBankOperationDto.class);

        assertThat(op.getDescription()).isEmpty(); // не null — иначе isBlank() в TBankConnector упал бы NPE
        assertThat(op.getPayPurpose()).isEqualTo("Оплата подписки");
    }
}
