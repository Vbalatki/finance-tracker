package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.mapper.BudgetMapper;
import com.finance.finance_tracker.mapper.CategoryMapper;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.repository.BudgetRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.service.BudgetService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;
    private final CategoryMapper categoryMapper;

    @Transactional
    public BudgetDto createBudget(BudgetDto dto) {
        Category category = categoryMapper.toEntity(findByCategoryId(dto.getCategoryId()));

        if (budgetRepository.existsByCategory(category)) {
            throw new IllegalArgumentException("Budget already exists for this category");
        }

        Budget budget = budgetMapper.toEntity(dto);
        budget.setCategory(category);
        budget.setCurrentSpending(BigDecimal.ZERO);

        Budget savedBudget = budgetRepository.save(budget);
        return budgetMapper.toDto(savedBudget);
    }

    @Transactional
    public List<BudgetDto> findByUserId(Long userId) {
        List<Budget> list = budgetRepository.findByUserId(userId);
        return list.stream()
                .map(budgetMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetDto updateBudget(Long id, BigDecimal newLimit) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        budget.setMonthlyLimit(newLimit);
        Budget updatedBudget = budgetRepository.save(budget);
        return budgetMapper.toDto(updatedBudget);
    }

    @Transactional
    public void resetBudgetSpending(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        budget.setCurrentSpending(BigDecimal.ZERO);
        budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }

    @Transactional
    public void addExpenseToBudget(Long categoryId, BigDecimal amount) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        budgetRepository.findByCategory(category).ifPresent(budget -> {
            BigDecimal newSpending = budget.getCurrentSpending().add(amount);
            budget.setCurrentSpending(newSpending);

            if (newSpending.compareTo(budget.getMonthlyLimit()) > 0) {
                notifyBudgetExceeded(budget);
            }

            budgetRepository.save(budget);
        });
    }

    private void notifyBudgetExceeded(Budget budget) {
        String message = String.format(
                "Budget exceeded for category %s: %s/%s",
                budget.getCategory().getName(),
                budget.getCurrentSpending(),
                budget.getMonthlyLimit()
        );
        System.out.println("ALERT: " + message);
    }

    private CategoryDto findByCategoryId(Long id) {
        return categoryMapper.toDto(categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found")));
    }

    private BudgetDto findById(Long id) {
        return budgetMapper.toDto(budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found")));
    }
}