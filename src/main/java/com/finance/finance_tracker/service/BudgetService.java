package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.BudgetDto;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetService {
    BudgetDto createBudget(BudgetDto dto);
    BudgetDto updateBudget(Long id, BigDecimal newLimit);
    List<BudgetDto> findByUserId(Long userId);
    void resetBudgetSpending(Long id);
    void deleteBudget(Long id);
    void addExpenseToBudget(Long categoryId, BigDecimal amount);
}
