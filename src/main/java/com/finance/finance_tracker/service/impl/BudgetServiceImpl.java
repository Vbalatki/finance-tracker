package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.BudgetDto;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.mapper.BudgetMapper;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.repository.BudgetRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.BudgetService;
import com.finance.finance_tracker.service.CurrencyApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.util.DataConstants.BUDGET_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.CATEGORY_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMapper budgetMapper;
    private final CurrencyApiService currencyApiService;

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

        // Границы текущего календарного месяца — для расчёта потраченного по каждой категории
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        List<Long> categoryIds = list.stream()
                .map(b -> b.getCategory().getId())
                .collect(Collectors.toList());

        Map<Long, BigDecimal> spentByCategory = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Object[]> rows = transactionRepository.findExpensesByCategoryIdsAndMonth(categoryIds, monthStart, monthEnd);
            for (Object[] row : rows) {
                Long categoryId = (Long) row[0];
                BigDecimal amount = (BigDecimal) row[1];
                Currency currency = (Currency) row[2];
                BigDecimal converted = currencyApiService.convertCurrency(currency.name(), Currency.RUB.name(), amount);
                spentByCategory.merge(categoryId, converted, BigDecimal::add);
            }
        }

        List<BudgetDto> budgets = list.stream().map(budget -> {
            BudgetDto dto = budgetMapper.toDto(budget);
            dto.setCurrentSpending(spentByCategory.getOrDefault(dto.getCategoryId(), BigDecimal.ZERO));
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
    public void deleteBudget(Long budgetId, Long currentUserId) {
        log.debug("Удаление бюджета: id={}, currentUserId={}", budgetId, currentUserId);

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> {
                    log.error("Бюджет не найден при удалении: id={}", budgetId);
                    return new EntityNotFoundException(BUDGET_NOT_FOUND + ", id: " + budgetId);
                });

        if (!budget.getUser().getId().equals(currentUserId)) {
            log.warn("Попытка удалить чужой бюджет: id={}, currentUserId={}, ownerId={}",
                    budgetId, currentUserId, budget.getUser().getId());
            throw new AccessDeniedException("Нет доступа к этому бюджету");
        }

        budgetRepository.delete(budget);
        log.info("Удалён бюджет с id: {}", budgetId);
    }
}
