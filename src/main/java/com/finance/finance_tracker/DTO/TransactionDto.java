package com.finance.finance_tracker.DTO;

import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDto {
    private Long id;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "TransactionType cannot be null")
    private TransactionType type;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private LocalDateTime createdAt;

    @NotNull(message = "AccountId cannot be null")
    private Long accountId;

    private Long categoryId;

    private String accountName;
    private String categoryName;
    private Currency accountCurrency;

}
