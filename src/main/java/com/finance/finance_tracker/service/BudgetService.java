package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.BudgetDto;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetService {
    List<BudgetDto> getBudgetsByUserId(Long userId);
    BudgetDto saveBudget(BudgetDto budgetDto, Long userId);
    void resetSpending(Long budgetId);
    void deleteBudget(Long budgetId);
}
