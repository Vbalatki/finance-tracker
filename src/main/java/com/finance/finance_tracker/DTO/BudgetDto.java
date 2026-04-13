package com.finance.finance_tracker.DTO;

import com.finance.finance_tracker.entity.Category;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetDto {
    private Long id;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "monthlyLimit must be positive")
    private BigDecimal monthlyLimit;

    @NotNull(message = "CurrentSpending cannot be null")
    @Positive(message = "CurrentSpending must be positive")
    private BigDecimal currentSpending;

    @NotNull(message = "CategoryId cannot be null")
    private Long categoryId;

    @OneToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
