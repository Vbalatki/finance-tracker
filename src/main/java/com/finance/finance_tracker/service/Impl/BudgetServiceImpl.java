package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.BudgetMapper;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.BudgetRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.Util.DataConstants.BUDGET_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.CATEGORY_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.INVALID_MONTHLY_LIMIT;
import static com.finance.finance_tracker.Util.DataConstants.USER_NOT_FOUND;

@Slf4j
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
        log.debug("Запрос бюджетов для пользователя с id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: id={}", userId);
                    return new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
                });

        List<Budget> list = budgetRepository.findByUserWithCategory(user);

        if (list.isEmpty()) {
            log.debug("У пользователя id={} нет бюджетов", userId);
        }

        List<BudgetDto> budgets = list.stream().map(budget -> {
            BudgetDto dto = budgetMapper.toDto(budget);
            BigDecimal spent = transactionRepository.getCurrentMonthExpenseByCategory(dto.getCategoryId());
            dto.setCurrentSpending(spent == null ? BigDecimal.ZERO : spent);
            return dto;
        }).collect(Collectors.toList());

        log.debug("Найдено бюджетов: {}", budgets.size());

        return budgets;
    }

    @Override
    @Transactional
    public BudgetDto saveBudget(BudgetDto budgetDto, Long userId) {
        log.debug("Сохранение бюджета: userId={}, categoryId={}, monthlyLimit={}",
                userId, budgetDto.getCategoryId(), budgetDto.getMonthlyLimit());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден при сохранении бюджета: id={}", userId);
                    return new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
                });

        Category category = categoryRepository.findById(budgetDto.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Категория не найдена при сохранении бюджета: id={}", budgetDto.getCategoryId());
                    return new EntityNotFoundException(CATEGORY_NOT_FOUND + ", id: " + budgetDto.getCategoryId());
                });

        Optional<Budget> existingBudget = budgetRepository.findByUserAndCategory(user, category);

        Budget budget;
        if (existingBudget.isPresent()) {
            log.debug("Обновление существующего бюджета: id={}", existingBudget.get().getId());
            budget = existingBudget.get();
            BigDecimal oldLimit = budget.getMonthlyLimit();
            budget.setMonthlyLimit(budgetDto.getMonthlyLimit());
            log.info("Обновлён бюджет: id={}, старый лимит={}, новый лимит={}",
                    budget.getId(), oldLimit, budget.getMonthlyLimit());
        } else {
            log.debug("Создание нового бюджета");
            budget = new Budget();
            budget.setUser(user);
            budget.setCategory(category);
            budget.setMonthlyLimit(budgetDto.getMonthlyLimit());
            budget.setCurrentSpending(BigDecimal.ZERO);
        }

        Budget saved = budgetRepository.save(budget);
        log.info("Сохранён бюджет: id={}, userId={}, categoryId={}, monthlyLimit={}",
                saved.getId(), userId, budgetDto.getCategoryId(), saved.getMonthlyLimit());

        return budgetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void resetSpending(Long budgetId) {
        log.debug("Сброс потраченной суммы для бюджета: id={}", budgetId);

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> {
                    log.error("Бюджет не найден при сбросе: id={}", budgetId);
                    return new EntityNotFoundException(BUDGET_NOT_FOUND + ", id: " + budgetId);
                });

        BigDecimal oldSpending = budget.getCurrentSpending();
        budget.setCurrentSpending(BigDecimal.ZERO);
        budgetRepository.save(budget);

        log.info("Сброшена потраченная сумма для бюджета id={}: было={}, стало=0", budgetId, oldSpending);
    }

    @Override
    @Transactional
    public void deleteBudget(Long budgetId) {
        log.debug("Удаление бюджета: id={}", budgetId);

        if (!budgetRepository.existsById(budgetId)) {
            log.error("Бюджет не найден при удалении: id={}", budgetId);
            throw new EntityNotFoundException(BUDGET_NOT_FOUND + ", id: " + budgetId);
        }

        budgetRepository.deleteById(budgetId);
        log.info("Удалён бюджет с id: {}", budgetId);
    }
}