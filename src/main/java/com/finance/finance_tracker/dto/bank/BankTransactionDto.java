package com.finance.finance_tracker.dto.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Нормализованное представление банковской транзакции — единый формат
 * для ЛЮБОГО банка-источника. Дальнейшая логика (дедупликация, импорт
 * в Transaction) работает только с этим типом.
 *
 * @param externalId    уникальный id операции в банке — обязателен для
 *                      дедупликации при повторном импорте того же периода
 * @param accountNumber номер счёта в банке, к которому относится операция
 * @param amount        сумма операции (положительная — пополнение,
 *                      отрицательная — списание; знак уже нормализован
 *                      коннектором, не зависит от того, как банк это
 *                      представляет у себя)
 * @param currency      код валюты, ISO 4217
 * @param description   назначение платежа/описание операции
 * @param bookingDate    дата проведения операции по счёту
 */
public record BankTransactionDto(
        String externalId,
        String accountNumber,
        BigDecimal amount,
        String currency,
        String description,
        LocalDateTime bookingDate
) {
}