package com.finance.finance_tracker.dto;

import com.finance.finance_tracker.entity.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountDto {
    private Long id;

    @NotBlank(message = "AccountName cannot be null")
    @Size(min = 2, max = 255)
    private String name;

    @NotNull(message = "Balance cannot be null")
    @PositiveOrZero
    private BigDecimal balance;

    @NotNull(message = "Currency cannot be null")
    private Currency currency;

    // Без @NotNull: userId никогда не должен приходить от клиента и всегда
    // проставляется в контроллере из SecurityContext (AccountController.createAccount)
    // ПОСЛЕ того, как отрабатывает @Valid. Если бы тут стоял @NotNull, форма без
    // userId (что и есть цель фикса IDOR) валилась бы с ошибкой валидации ещё до
    // того, как контроллер успевал его проставить.
    private Long userId;

    private String bankCode;
    private String externalAccountNumber;
    private LocalDateTime lastSyncedAt;
}