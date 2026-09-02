package com.finance.finance_tracker.service;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.dto.ChangePasswordDto;
import com.finance.finance_tracker.dto.TransactionDto;
import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.dto.UserSettingsDto;

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
     * пользователь создаётся активным. Уникальность email проверяется
     * Bean Validation ({@link com.finance.finance_tracker.validation.UniqueEmail}
     * на {@link UserDto}) до вызова этого метода.
     *
     * @param dto данные регистрации
     * @return созданный пользователь
     */
    UserDto registerUser(UserDto dto);

    /**
     * Возвращает пользователя по id.
     *
     * @param id id пользователя
     * @return dto пользователя
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
     * Пароль этим методом не меняется — см. {@link #changePassword(Long, ChangePasswordDto)}.
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
     * Меняет пароль пользователя: проверяет текущий пароль, длину нового
     * пароля и совпадение с подтверждением проверяет Bean Validation на
     * {@link ChangePasswordDto} до вызова этого метода.
     *
     * @param userId id пользователя
     * @param dto    текущий пароль, новый пароль и его подтверждение
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     * @throws com.finance.finance_tracker.exception.InvalidDataException если текущий пароль неверен
     */
    void changePassword(Long userId, ChangePasswordDto dto);

    /**
     * Возвращает пользователя по email.
     *
     * @param email email пользователя
     * @return dto пользователя
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    UserDto getUserByEmail(String email);

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
     * в пересчёте на рубли.
     *
     * @param list список транзакций (может быть {@code null} или пустым — тогда результат 0)
     * @return суммарный доход в рублях
     */
    BigDecimal getUserTotalIncomeInRub(List<TransactionDto> list);

    /**
     * Считает суммарный расход ({@code EXPENSE}) по списку транзакций
     * в пересчёте на рубли.
     *
     * @param list список транзакций (может быть {@code null} или пустым — тогда результат 0)
     * @return суммарный расход в рублях
     */
    BigDecimal getUserTotalExpenseInRub(List<TransactionDto> list);

    /**
     * Возвращает текущие настройки пользователя.
     *
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    UserSettingsDto getUserSettings(Long userId);

    /**
     * Обновляет настройки пользователя.
     *
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     */
    void updateUserSettings(Long userId, UserSettingsDto dto);
}