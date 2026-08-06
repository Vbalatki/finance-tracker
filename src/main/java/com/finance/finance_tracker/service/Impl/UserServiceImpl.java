package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.dto.TransactionDto;
import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.dto.UserSettingsDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Role;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.AccountMapper;
import com.finance.finance_tracker.mapper.UserMapper;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.RoleRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.CurrencyApiService;
import com.finance.finance_tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.util.DataConstants.ACCOUNT_NAME_EXISTS;
import static com.finance.finance_tracker.util.DataConstants.EMAIL_ALREADY_EXISTS;
import static com.finance.finance_tracker.util.DataConstants.INCORRECT_CURRENT_PASSWORD;
import static com.finance.finance_tracker.util.DataConstants.MIN_PASSWORD_LENGTH;
import static com.finance.finance_tracker.util.DataConstants.PASSWORD_TOO_SHORT;
import static com.finance.finance_tracker.util.DataConstants.ROLE_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final CurrencyApiService currencyApiService;



    @Transactional
    public UserDto registerUser(UserDto dto) {
        log.debug("Регистрация нового пользователя: email={}", dto.getEmail());

        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Попытка регистрации с уже существующим email: {}", dto.getEmail());
            throw new DuplicateEntityException(EMAIL_ALREADY_EXISTS + ": " + dto.getEmail());
        }

        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setActive(true);

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new EntityNotFoundException("Дефолтная роль пользователя не найдена"));
        user.setRoles(new HashSet<>(Set.of(defaultRole)));

        User savedUser = userRepository.save(user);
        log.info("Зарегистрирован новый пользователь: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        log.debug("Поиск пользователя по email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден по email: {}", email);
                    return new EntityNotFoundException(USER_NOT_FOUND + " с email: " + email);
                });
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.debug("Поиск пользователя по id: {}", id);
        User user = findById(id);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        log.debug("Запрос всех пользователей");
        List<User> users = userRepository.findAll();
        log.debug("Найдено пользователей: {}", users.size());
        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto dto) {
        log.debug("Обновление пользователя: id={}", id);
        User user = findById(id);

        String oldEmail = user.getEmail();
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setBirthday(dto.getBirthday());
        user.setEmail(dto.getEmail());

        User updatedUser = userRepository.save(user);
        log.info("Обновлён пользователь: id={}, email changed: {} -> {}", id, oldEmail, dto.getEmail());

        return userMapper.toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.debug("Удаление пользователя: id={}", id);
        User user = findById(id);
        userRepository.delete(user);
        log.info("Удалён пользователь: id={}, email={}", id, user.getEmail());
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        log.debug("Назначение ролей пользователю: userId={}, roleIds={}", userId, roleIds);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден при назначении ролей: id={}", userId);
                    return new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
                });
        Set<Role> roles = roleIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> {
                            log.error("Роль не найдена при назначении: roleId={}", roleId);
                            return new EntityNotFoundException(ROLE_NOT_FOUND + ", id: " + roleId);
                        }))
                .collect(Collectors.toSet());
        user.setRoles(roles);
        userRepository.save(user);
        log.info("Назначены роли пользователю id={}: {}", userId, roles.stream().map(Role::getName).collect(Collectors.toSet()));
    }

    @Transactional
    public void toggleActive(Long userId) {
        log.debug("Переключение статуса активности пользователя: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден при переключении статуса: id={}", userId);
                    return new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
                });
        boolean oldStatus = user.isActive();
        user.setActive(!user.isActive());
        userRepository.save(user);
        log.info("Статус пользователя id={} изменён: {} -> {}", userId, oldStatus, user.isActive());
    }

    @Transactional(readOnly = true)
    public Set<Long> getUserRoleIds(Long userId) {
        log.debug("Получение ID ролей пользователя: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден при получении ролей: id={}", userId);
                    return new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
                });
        Set<Long> roleIds = user.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        log.debug("Найдены роли пользователя {}: {}", userId, roleIds);
        return roleIds;
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.debug("Смена пароля для пользователя: userId={}", userId);
        User user = findById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Неудачная попытка смены пароля для пользователя {}: неверный текущий пароль", userId);
            throw new InvalidDataException(INCORRECT_CURRENT_PASSWORD);
        }

        if (newPassword.length() < Integer.parseInt(MIN_PASSWORD_LENGTH)) {
            log.warn("Неудачная попытка смены пароля для пользователя {}: пароль слишком короткий", userId);
            throw new InvalidDataException(PASSWORD_TOO_SHORT);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Пароль изменён для пользователя: id={}", userId);
    }

    @Transactional
    public AccountDto addAccountToUser(Long userId, AccountDto accountDto) {
        log.debug("Добавление счёта пользователю: userId={}, accountName={}", userId, accountDto.getName());
        User user = findById(userId);

        if (accountRepository.existsByNameAndUserId(accountDto.getName(), user.getId())) {
            log.warn("Попытка создать счёт с дублирующимся именем: userId={}, name={}", userId, accountDto.getName());
            throw new DuplicateEntityException(ACCOUNT_NAME_EXISTS + ": " + accountDto.getName());
        }

        Account account = accountMapper.toEntity(accountDto);
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);

        Account savedAccount = accountRepository.save(account);
        user.getAccounts().add(savedAccount);

        log.info("Создан новый счёт для пользователя {}: accountId={}, name={}", userId, savedAccount.getId(), savedAccount.getName());
        return accountMapper.toDto(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getUserAccounts(Long userId) {
        log.debug("Запрос счетов пользователя: userId={}", userId);
        User user = findById(userId);
        List<AccountDto> accounts = user.getAccounts().stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Найдено счетов: {}", accounts.size());
        return accounts;
    }

    @Transactional(readOnly = true)
    public BigDecimal getUserTotalBalanceInRub(List<AccountDto> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            log.debug("Пустой список счетов для расчёта баланса в рублях");
            return BigDecimal.ZERO;
        }

        BigDecimal totalInRub = BigDecimal.ZERO;
        for (AccountDto dto : accounts) {
            BigDecimal balance = dto.getBalance();
            Currency currency = dto.getCurrency();
            BigDecimal rubAmount = currency == Currency.RUB
                                    ? balance
                                    : currencyApiService
                                        .convertCurrency(currency.name(), "RUB", balance);
            totalInRub = totalInRub.add(rubAmount);
        }
        log.debug("Общий баланс в рублях: {}", totalInRub);
        return totalInRub;
    }

    @Transactional(readOnly = true)
    public BigDecimal getUserTotalIncomeInRub(List<TransactionDto> transactions) {
        return getUserTotalIncomeOrExpenseInRub(transactions, TransactionType.INCOME);
    }

    @Transactional(readOnly = true)
    public BigDecimal getUserTotalExpenseInRub(List<TransactionDto> transactions) {
        return getUserTotalIncomeOrExpenseInRub(transactions, TransactionType.EXPENSE);
    }


    private BigDecimal getUserTotalIncomeOrExpenseInRub(List<TransactionDto> transactions,
                                                       TransactionType type) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (TransactionDto t : transactions) {
            if (t.getType() != type) {
                continue;
            }

            Currency currency = t.getAccountCurrency();
            if (currency == null) {
                log.warn("Currency is null for transaction id {}, treating as RUB", t.getId());
                currency = Currency.RUB;
            }

            BigDecimal amountInRub = currency == Currency.RUB
                    ? t.getAmount()
                    : currencyApiService.convertCurrency(currency.name(), "RUB", t.getAmount());

            total = total.add(amountInRub);
        }
        return total;
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: id={}", id);
                    return new EntityNotFoundException(USER_NOT_FOUND + ", id: " + id);
                });
    }

    private BigDecimal convertToRub(BigDecimal amount, Currency currency) {
        if (currency == null || currency == Currency.RUB) {
            return amount;
        }

        BigDecimal rate = currencyApiService.convertCurrency(currency.name(), "RUB", BigDecimal.ONE);
        return amount.multiply(rate);
    }

    @Transactional(readOnly = true)
    public UserSettingsDto getUserSettings(Long userId) {
        User user = findById(userId);
        UserSettingsDto dto = new UserSettingsDto();
        dto.setTheme(user.getTheme());
        dto.setDefaultCurrency(user.getDefaultCurrency());
        dto.setLocale(user.getLocale());
        return dto;
    }

    @Override
    @Transactional
    public void updateUserSettings(Long userId, UserSettingsDto dto) {
        User user = findById(userId);
        user.setTheme(dto.getTheme());
        user.setDefaultCurrency(dto.getDefaultCurrency());
        user.setLocale(dto.getLocale());
        userRepository.save(user);
        log.info("Обновлены настройки пользователя id={}: theme={}, defaultCurrency={}, locale={}",
                userId, dto.getTheme(), dto.getDefaultCurrency(), dto.getLocale());
    }
}