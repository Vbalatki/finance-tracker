package com.finance.finance_tracker.dto;

import com.finance.finance_tracker.entity.enums.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecurringCommitmentDto {

    private Long id;

    @NotBlank(message = "Название обязательно")
    private String name;

    @NotNull(message = "Сумма обязательна")
    @Positive(message = "Сумма должна быть положительной")
    private BigDecimal amount;

    @NotNull(message = "Тип обязателен")
    private TransactionType type;

    @NotNull(message = "День месяца обязателен")
    @Min(value = 1, message = "День месяца от 1 до 31")
    @Max(value = 31, message = "День месяца от 1 до 31")
    private Integer dayOfMonth;

    private Long categoryId;
    private String categoryName;

    private Long accountId;
    private String accountName;

    private boolean active = true;

    private Long userId;
}