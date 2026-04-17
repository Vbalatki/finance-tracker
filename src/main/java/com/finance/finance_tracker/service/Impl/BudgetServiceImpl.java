package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.mapper.BudgetMapper;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.repository.BudgetRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.BudgetService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMapper budgetMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BudgetDto> getBudgetsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден, id: " + userId));
        List<Budget> list = budgetRepository.findByUserWithCategory(user);

        return list.stream().map(budget -> {
            BudgetDto dto = budgetMapper.toDto(budget);
            BigDecimal spent = transactionRepository.getCurrentMonthExpenseByCategory(dto.getCategoryId());
            System.out.println("Category ID: " + dto.getCategoryId() + ", spent from DB: " + spent);
            dto.setCurrentSpending(spent == null ? BigDecimal.ZERO : spent);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BudgetDto saveBudget(BudgetDto budgetDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден, id: " + userId));
        Category category = categoryRepository.findById(budgetDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена, id: " + budgetDto.getCategoryId()));

        Optional<Budget> existingBudget = budgetRepository.findByUserAndCategory(user, category);

        Budget budget;
        if (existingBudget.isPresent()) {
            budget = existingBudget.get();
            budget.setMonthlyLimit(budgetDto.getMonthlyLimit());
        } else {
            budget = new Budget();
            budget.setUser(user);
            budget.setCategory(category);
            budget.setMonthlyLimit(budgetDto.getMonthlyLimit());
            budget.setCurrentSpending(BigDecimal.ZERO);
        }

        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void resetSpending(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Бюджет не найден, id: " + budgetId));
        budget.setCurrentSpending(BigDecimal.ZERO);
        budgetRepository.save(budget);
    }

    @Override
    @Transactional
    public void deleteBudget(Long budgetId) {
        if (!budgetRepository.existsById(budgetId)) {
            throw new IllegalArgumentException("Бюджет не найден, id: " + budgetId);
        }
        budgetRepository.deleteById(budgetId);
    }
}