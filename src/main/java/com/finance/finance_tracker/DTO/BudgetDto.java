package com.finance.finance_tracker.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetDto {

    private Long id;

    @NotNull(message = "Monthly limit cannot be null")
    @Positive(message = "Monthly limit must be positive")
    private BigDecimal monthlyLimit;

    @PositiveOrZero(message = "Current spending must be positive or zero")
    private BigDecimal currentSpending = BigDecimal.ZERO;

    @NotNull(message = "Category ID cannot be null")
    private Long categoryId;

    private String categoryName;
}