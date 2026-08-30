package com.finance.finance_tracker.dto;

import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.validation.UniqueAccountName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@UniqueAccountName
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

    // @NotNull снят другим агентом 2026-08-13 (см. Audit/Patch Log) — поле
    // не должно приходить от клиента, ставится в AccountController.createAccount
    // из SecurityContext уже ПОСЛЕ @Valid. UniqueAccountNameValidator этого
    // поля тоже не читает, использует SecurityUtil.getCurrentUserId() — по той
    // же причине.
    private Long userId;

    private String bankCode;
    private String externalAccountNumber;
    private LocalDateTime lastSyncedAt;
}
