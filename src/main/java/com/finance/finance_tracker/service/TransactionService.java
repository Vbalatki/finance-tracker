package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.TransactionDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Управление финансовыми транзакциями (доходы/расходы) и их влиянием
 * на баланс связанных счетов.
 */
public interface TransactionService {

    /**
     * Возвращает транзакцию по id.
     *
     * @param id id транзакции
     * @return DTO транзакции
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если транзакция не найдена
     */
    TransactionDto findById(Long id);

    /**
     * Частично обновляет транзакцию: изменяются только те поля {@code dto},
     * которые заданы (не {@code null} и отличаются от текущих значений).
     * Если сумма, тип или счёт изменились — баланс затронутых счетов
     * пересчитывается (старый эффект транзакции отменяется, новый применяется).
     *
     * @param dto данные для обновления, {@code dto.id} обязателен
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если транзакция, новый счёт или новая категория не найдены
     * @throws com.finance.finance_tracker.exception.InvalidAmountException если новая сумма не положительна
     */
    void updateTransaction(TransactionDto dto);

    /**
     * Создаёт новую транзакцию и сразу применяет её эффект к балансу счёта
     * (прибавляет для {@code INCOME}, вычитает для {@code EXPENSE}).
     *
     * @param dto данные новой транзакции
     * @return созданная транзакция
     * @throws com.finance.finance_tracker.exception.InvalidDataException если не указан счёт или тип транзакции
     * @throws com.finance.finance_tracker.exception.InvalidAmountException если сумма не положительна
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если счёт или категория не найдены
     */
    TransactionDto saveTransaction(TransactionDto dto);

    /**
     * Возвращает транзакцию по id. Функционально идентичен {@link #findById(Long)}.
     *
     * @param id id транзакции
     * @return DTO транзакции
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если транзакция не найдена
     */
    TransactionDto getTransactionById(Long id);

    /**
     * Возвращает все транзакции пользователя по всем его счетам, отсортированные
     * по дате создания (сначала новые).
     *
     * @param userId id пользователя
     * @return список транзакций
     */
    List<TransactionDto> getUserTransactions(Long userId);

    /**
     * Возвращает транзакции конкретного счёта.
     *
     * @param accountId id счёта
     * @return список транзакций счёта
     */
    List<TransactionDto> findByAccountId(Long accountId);

    /**
     * Возвращает все транзакции пользователя. Функционально идентичен
     * {@link #getUserTransactions(Long)}.
     *
     * @param userId id пользователя
     * @return список транзакций
     */
    List<TransactionDto> findByUserId(Long userId);

    /**
     * Возвращает транзакции, привязанные к указанной категории.
     *
     * @param categoryId id категории
     * @return список транзакций категории
     */
    List<TransactionDto> getTransactionsByCategory(Long categoryId);

    /**
     * Считает баланс пользователя как разницу между суммой всех его
     * доходов и суммой всех расходов по всем счетам.
     *
     * @param userId id пользователя
     * @return баланс (может быть отрицательным)
     */
    BigDecimal calculateUserBalance(Long userId);

    /**
     * Возвращает последние {@code recents} транзакций пользователя по всем
     * его счетам, с проставленными именем и валютой счёта.
     *
     * @param userId  id пользователя
     * @param recents максимальное количество возвращаемых транзакций
     * @return список последних транзакций, отсортированный по дате (сначала новые)
     */
    List<TransactionDto> findRecentByUserId(Long userId, int recents);

    /**
     * Удаляет транзакцию и откатывает её эффект на балансе связанного счёта
     * (вычитает, если это было {@code INCOME}, прибавляет — если {@code EXPENSE}).
     *
     * @param id id транзакции
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если транзакция не найдена
     * @throws com.finance.finance_tracker.exception.InvalidDataException если у транзакции отсутствует счёт
     */
    void deleteTransaction(Long id);
}
