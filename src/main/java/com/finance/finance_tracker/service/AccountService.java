package com.finance.finance_tracker.service;


import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.entity.enums.Currency;

import java.math.BigDecimal;
import java.util.List;

/**
 * Управление банковскими счетами пользователей: создание, пополнение,
 * снятие средств и расчёт суммарного баланса.
 */
public interface AccountService {

    /**
     * Возвращает счёт по идентификатору.
     *
     * @param id id счёта
     * @return DTO счёта
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если счёт не найден
     */
    AccountDto findById(Long id);

    /**
     * Создаёт новый счёт для пользователя, указанного в {@code dto.userId}.
     * Если баланс в {@code dto} не задан, устанавливается 0.
     *
     * @param dto данные создаваемого счёта
     * @return созданный счёт
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если у пользователя уже есть счёт с таким именем
     */
    AccountDto saveAccount(AccountDto dto);

    /**
     * Пополняет счёт на указанную сумму.
     *
     * @param accountId id счёта
     * @param amount    сумма пополнения, должна быть положительной
     * @return счёт с обновлённым балансом
     * @throws com.finance.finance_tracker.exception.InvalidAmountException если сумма null, ноль или отрицательная
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если счёт не найден
     */
    AccountDto deposit(Long accountId, BigDecimal amount);

    /**
     * Снимает средства со счёта на указанную сумму.
     *
     * @param accountId id счёта
     * @param amount    сумма снятия, должна быть положительной и не превышать баланс
     * @return счёт с обновлённым балансом
     * @throws com.finance.finance_tracker.exception.InvalidAmountException если сумма null, ноль или отрицательная
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если счёт не найден
     * @throws com.finance.finance_tracker.exception.InsufficientFundsException если на счёте недостаточно средств
     */
    AccountDto withdraw(Long accountId, BigDecimal amount);

    /**
     * Возвращает все счета пользователя.
     *
     * @param userId id пользователя
     * @return список счетов (может быть пустым)
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    List<AccountDto> getUserAccounts(Long userId);

    /**
     * Удаляет счёт вместе со всеми связанными с ним транзакциями.
     *
     * @param id id счёта
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если счёт не найден
     */
    void deleteAccount(Long id);

    /**
     * Возвращает суммарный баланс всех счетов пользователя (без пересчёта валют).
     *
     * @param userId id пользователя
     * @return суммарный баланс, 0 если счетов нет
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    BigDecimal getTotalBalance(Long userId);

    /**
     * Возвращает суммарный баланс всех счетов пользователя.
     *
     * <p><b>Внимание:</b> на текущий момент параметр {@code currency} не
     * используется — метод фактически возвращает тот же результат, что и
     * {@link #getTotalBalance(Long)}, без конвертации в указанную валюту.
     *
     * @param userId   id пользователя
     * @param currency целевая валюта (пока не влияет на результат)
     * @return суммарный баланс
     */
    BigDecimal getTotalBalanceInCurrency(Long userId, Currency currency);

    /**
     * Обновляет имя и валюту счёта. Баланс через этот метод не меняется —
     * для этого есть {@link #deposit(Long, BigDecimal)} и {@link #withdraw(Long, BigDecimal)}.
     *
     * @param id  id счёта
     * @param dto новые значения имени/валюты
     * @return обновлённый счёт
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если счёт не найден
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если новое имя уже занято другим счётом пользователя
     * @throws com.finance.finance_tracker.exception.InvalidDataException если валюта не указана
     */
    AccountDto updateAccount(Long id, AccountDto dto);
}
