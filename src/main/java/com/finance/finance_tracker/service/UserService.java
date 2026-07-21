package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.DTO.UserDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Управление пользователями: регистрация, профиль, роли, пароль,
 * а также агрегированные расчёты (суммарный баланс, доходы/расходы
 * в пересчёте на рубли).
 */
public interface UserService {

    /**
     * Регистрирует нового пользователя. Пароль сохраняется в закодированном
     * виде ({@link org.springframework.security.crypto.password.PasswordEncoder}),
     * пользователь создаётся активным.
     *
     * @param dto данные регистрации
     * @return созданный пользователь
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если email уже зарегистрирован
     */
    UserDto registerUser(UserDto dto);

    /**
     * Возвращает пользователя по id.
     *
     * @param id id пользователя
     * @return DTO пользователя
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    UserDto getUserById(Long id);

    /**
     * Возвращает всех пользователей системы.
     *
     * @return список всех пользователей
     */
    List<UserDto> getAllUsers();

    /**
     * Обновляет базовые данные профиля (имя, фамилию, дату рождения, email).
     * Пароль этим методом не меняется — см. {@link #changePassword(Long, String, String)}.
     *
     * @param id  id пользователя
     * @param dto новые значения полей
     * @return обновлённый пользователь
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    UserDto updateUser(Long id, UserDto dto);

    /**
     * Удаляет пользователя.
     *
     * @param id id пользователя
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    void deleteUser(Long id);

    /**
     * Полностью заменяет набор ролей пользователя на переданный.
     *
     * @param userId  id пользователя
     * @param roleIds id ролей, которые должны остаться у пользователя
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь или одна из ролей не найдены
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * Переключает статус активности пользователя (активен/заблокирован).
     *
     * @param userId id пользователя
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    void toggleActive(Long userId);

    /**
     * Возвращает id ролей, назначенных пользователю.
     *
     * @param userId id пользователя
     * @return множество id ролей
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    Set<Long> getUserRoleIds(Long userId);

    /**
     * Меняет пароль пользователя, предварительно проверяя текущий пароль.
     *
     * @param userId          id пользователя
     * @param currentPassword текущий (нешифрованный) пароль для проверки
     * @param newPassword     новый пароль, минимум 8 символов
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     * @throws com.finance.finance_tracker.exception.InvalidDataException если текущий пароль неверен или новый короче 8 символов
     */
    void changePassword(Long userId, String currentPassword, String newPassword);

    /**
     * Возвращает пользователя по email.
     *
     * @param email email пользователя
     * @return DTO пользователя
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    UserDto getUserByEmail(String email);

    /**
     * Добавляет пользователю новый счёт с нулевым начальным балансом
     * (значение {@code dto.balance} игнорируется).
     *
     * @param userId id пользователя
     * @param dto    данные счёта (используются только имя и валюта)
     * @return созданный счёт
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если у пользователя уже есть счёт с таким именем
     */
    AccountDto addAccountToUser(Long userId, AccountDto dto);

    /**
     * Возвращает все счета пользователя.
     *
     * @param userId id пользователя
     * @return список счетов
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    List<AccountDto> getUserAccounts(Long userId);

    /**
     * Считает суммарный баланс списка счетов в пересчёте на рубли,
     * используя актуальный курс из {@link CurrencyApiService}.
     *
     * @param list список счетов (может быть {@code null} или пустым — тогда результат 0)
     * @return суммарный баланс в рублях
     */
    BigDecimal getUserTotalBalanceInRub(List<AccountDto> list);

    /**
     * Считает суммарный доход ({@code INCOME}) по списку транзакций
     * в пересчёте на рубли. Для рублёвых транзакций конвертация не
     * вызывается (короткий путь без обращения к внешнему API курсов).
     *
     * @param list список транзакций (может быть {@code null} или пустым — тогда результат 0)
     * @return суммарный доход в рублях
     */
    BigDecimal getUserTotalIncomeInRub(List<TransactionDto> list);

    /**
     * Считает суммарный расход ({@code EXPENSE}) по списку транзакций
     * в пересчёте на рубли.
     *
     * <p><b>Внимание:</b> в отличие от {@link #getUserTotalIncomeInRub(List)},
     * этот метод обращается к {@link CurrencyApiService} даже для уже
     * рублёвых транзакций — это несогласованность в реализации, а не
     * намеренная оптимизация.
     *
     * @param list список транзакций (может быть {@code null} или пустым — тогда результат 0)
     * @return суммарный расход в рублях
     */
    BigDecimal getUserTotalExpenseInRub(List<TransactionDto> list);
}
