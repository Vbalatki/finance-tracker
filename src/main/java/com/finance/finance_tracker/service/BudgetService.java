package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.BudgetDto;

import java.util.List;

/**
 * Управление месячными бюджетами по категориям расходов.
 */
public interface BudgetService {

    /**
     * Возвращает все бюджеты пользователя с проставленной текущей суммой
     * трат за месяц (вычисляется на лету по транзакциям категории,
     * а не берётся из сохранённого поля {@code currentSpending}).
     *
     * @param userId id пользователя
     * @return список бюджетов пользователя
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    List<BudgetDto> getBudgetsByUserId(Long userId);

    /**
     * Создаёт бюджет для категории или обновляет месячный лимит
     * существующего бюджета этой же пары пользователь/категория.
     *
     * @param budgetDto данные бюджета
     * @param userId    id пользователя-владельца бюджета
     * @return сохранённый бюджет
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь или категория не найдены
     */
    BudgetDto saveBudget(BudgetDto budgetDto, Long userId);

    /**
     * Обнуляет накопленную сумму трат ({@code currentSpending}) бюджета.
     *
     * @param budgetId id бюджета
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если бюджет не найден
     */
    void resetSpending(Long budgetId);

    /**
     * Удаляет бюджет.
     *
     * @param budgetId id бюджета
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если бюджет не найден
     */
    void deleteBudget(Long budgetId);
}
