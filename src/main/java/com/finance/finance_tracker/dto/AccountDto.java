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

    @NotNull(message = "UserId cannot be null")
    private Long userId;

    private String bankCode;
    private String externalAccountNumber;
    private LocalDateTime lastSyncedAt;
}