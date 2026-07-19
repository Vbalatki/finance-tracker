package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.DTO.UserDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link UserServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private CurrencyApiService currencyApiService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Иван");
        user.setSurname("Иванов");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setEmail("ivan@example.com");
        user.setPassword("encodedOldPassword");
        user.setActive(true);

        userDto = new UserDto();
        userDto.setName("Иван");
        userDto.setSurname("Иванов");
        userDto.setBirthday(LocalDate.of(1990, 1, 1));
        userDto.setEmail("ivan@example.com");
        userDto.setPassword("password123");
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("создаёт пользователя с закодированным паролем и active=true")
        void registerUser_success() {
            when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
            when(userMapper.toEntity(userDto)).thenReturn(user);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(userDto);

            userService.registerUser(userDto);

            verify(userRepository).save(argThat(u -> "encoded".equals(u.getPassword()) && u.isActive()));
        }

        @Test
        @DisplayName("бросает DuplicateEntityException, если email уже занят")
        void registerUser_duplicateEmail_throws() {
            when(userRepository.existsByEmail("ivan@example.com")).thenReturn(true);

            assertThrows(DuplicateEntityException.class, () -> userService.registerUser(userDto));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("поиск пользователей")
    class FindUsers {

        @Test
        @DisplayName("getUserByEmail возвращает пользователя, если найден")
        void getUserByEmail_success() {
            when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(userDto);

            assertThat(userService.getUserByEmail("ivan@example.com")).isEqualTo(userDto);
        }

        @Test
        @DisplayName("getUserByEmail бросает EntityNotFoundException, если не найден")
        void getUserByEmail_notFound_throws() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.getUserByEmail("unknown@example.com"));
        }

        @Test
        @DisplayName("getUserById возвращает пользователя")
        void getUserById_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(userDto);

            assertThat(userService.getUserById(1L)).isEqualTo(userDto);
        }

        @Test
        @DisplayName("getAllUsers возвращает всех пользователей")
        void getAllUsers_success() {
            when(userRepository.findAll()).thenReturn(List.of(user));
            when(userMapper.toDto(user)).thenReturn(userDto);

            assertThat(userService.getAllUsers()).containsExactly(userDto);
        }
    }

    @Nested
    @DisplayName("updateUser / deleteUser")
    class UpdateAndDelete {

        @Test
        @DisplayName("обновляет основные поля пользователя")
        void updateUser_success() {
            UserDto update = new UserDto();
            update.setName("Пётр");
            update.setSurname("Петров");
            update.setBirthday(LocalDate.of(1991, 2, 2));
            update.setEmail("petr@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(update);

            userService.updateUser(1L, update);

            assertThat(user.getName()).isEqualTo("Пётр");
            assertThat(user.getEmail()).isEqualTo("petr@example.com");
        }

        @Test
        @DisplayName("бросает EntityNotFoundException при обновлении несуществующего пользователя")
        void updateUser_notFound_throws() {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.updateUser(404L, userDto));
        }

        @Test
        @DisplayName("удаляет пользователя")
        void deleteUser_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.deleteUser(1L);

            verify(userRepository).delete(user);
        }
    }

    @Nested
    @DisplayName("роли и активность")
    class RolesAndActivity {

        @Test
        @DisplayName("assignRoles назначает набор ролей пользователю")
        void assignRoles_success() {
            Role role = new Role();
            role.setId(2L);
            role.setName("ROLE_ADMIN");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.assignRoles(1L, List.of(2L));

            assertThat(user.getRoles()).containsExactly(role);
        }

        @Test
        @DisplayName("assignRoles бросает EntityNotFoundException для неизвестной роли")
        void assignRoles_roleNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> userService.assignRoles(1L, List.of(99L)));
        }

        @Test
        @DisplayName("toggleActive переключает статус активности")
        void toggleActive_flipsStatus() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            userService.toggleActive(1L);

            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("getUserRoleIds возвращает id ролей пользователя")
        void getUserRoleIds_success() {
            Role role = new Role();
            role.setId(3L);
            user.setRoles(Set.of(role));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThat(userService.getUserRoleIds(1L)).containsExactly(3L);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("меняет пароль при верном текущем и достаточно длинном новом")
        void changePassword_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass", "encodedOldPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNew");
            when(userRepository.save(user)).thenReturn(user);

            userService.changePassword(1L, "oldPass", "newPassword123");

            assertThat(user.getPassword()).isEqualTo("encodedNew");
        }

        @Test
        @DisplayName("бросает InvalidDataException при неверном текущем пароле")
        void changePassword_wrongCurrentPassword_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encodedOldPassword")).thenReturn(false);

            assertThrows(InvalidDataException.class,
                    () -> userService.changePassword(1L, "wrong", "newPassword123"));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("бросает InvalidDataException, если новый пароль короче 8 символов")
        void changePassword_tooShort_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass", "encodedOldPassword")).thenReturn(true);

            assertThrows(InvalidDataException.class, () -> userService.changePassword(1L, "oldPass", "123"));
        }
    }

    @Nested
    @DisplayName("счета пользователя")
    class UserAccounts {

        @Test
        @DisplayName("addAccountToUser создаёт счёт с нулевым балансом")
        void addAccountToUser_success() {
            AccountDto dto = new AccountDto();
            dto.setName("Новый счет");
            dto.setBalance(new BigDecimal("500.00"));
            dto.setCurrency(Currency.RUB);

            Account entity = new Account();
            entity.setName("Новый счет");

            Account saved = new Account();
            saved.setId(50L);
            saved.setName("Новый счет");
            saved.setBalance(BigDecimal.ZERO);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(accountRepository.existsByNameAndUserId("Новый счет", 1L)).thenReturn(false);
            when(accountMapper.toEntity(dto)).thenReturn(entity);
            when(accountRepository.save(entity)).thenReturn(saved);
            when(accountMapper.toDto(saved)).thenReturn(dto);

            userService.addAccountToUser(1L, dto);

            assertThat(entity.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(entity.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("addAccountToUser бросает DuplicateEntityException при дублирующемся имени")
        void addAccountToUser_duplicateName_throws() {
            AccountDto dto = new AccountDto();
            dto.setName("Занято");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(accountRepository.existsByNameAndUserId("Занято", 1L)).thenReturn(true);

            assertThrows(DuplicateEntityException.class, () -> userService.addAccountToUser(1L, dto));
        }
    }

    @Nested
    @DisplayName("подсчёт балансов в рублях")
    class BalanceCalculations {

        @Test
        @DisplayName("суммирует балансы счетов с конвертацией в рубли")
        void getUserTotalBalanceInRub_convertsEachAccount() {
            AccountDto rubAcc = new AccountDto();
            rubAcc.setBalance(new BigDecimal("100.00"));
            rubAcc.setCurrency(Currency.RUB);

            AccountDto usdAcc = new AccountDto();
            usdAcc.setBalance(new BigDecimal("10.00"));
            usdAcc.setCurrency(Currency.USD);

            when(currencyApiService.convertCurrency("RUB", "RUB", new BigDecimal("100.00")))
                    .thenReturn(new BigDecimal("100.00"));
            when(currencyApiService.convertCurrency("USD", "RUB", new BigDecimal("10.00")))
                    .thenReturn(new BigDecimal("900.00"));

            BigDecimal result = userService.getUserTotalBalanceInRub(List.of(rubAcc, usdAcc));

            assertThat(result).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("возвращает 0 для пустого/null списка счетов")
        void getUserTotalBalanceInRub_emptyOrNull_returnsZero() {
            assertThat(userService.getUserTotalBalanceInRub(List.of())).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(userService.getUserTotalBalanceInRub(null)).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(currencyApiService);
        }

        @Test
        @DisplayName("для дохода в рублях НЕ вызывает конвертацию (короткий путь)")
        void getUserTotalIncomeInRub_rubCurrency_doesNotCallConversionApi() {
            TransactionDto t = new TransactionDto();
            t.setType(TransactionType.INCOME);
            t.setAmount(new BigDecimal("100.00"));
            t.setAccountCurrency(Currency.RUB);

            BigDecimal result = userService.getUserTotalIncomeInRub(List.of(t));

            assertThat(result).isEqualByComparingTo("100.00");
            verifyNoInteractions(currencyApiService);
        }

        @Test
        @DisplayName("доход в иностранной валюте конвертируется по курсу")
        void getUserTotalIncomeInRub_foreignCurrency_convertsUsingRate() {
            TransactionDto t = new TransactionDto();
            t.setType(TransactionType.INCOME);
            t.setAmount(new BigDecimal("10.00"));
            t.setAccountCurrency(Currency.USD);

            when(currencyApiService.convertCurrency("USD", "RUB", BigDecimal.ONE))
                    .thenReturn(new BigDecimal("90.00"));

            BigDecimal result = userService.getUserTotalIncomeInRub(List.of(t));

            assertThat(result).isEqualByComparingTo("900.00");
        }

        @Test
        @DisplayName("НЕСОГЛАСОВАННОСТЬ: для расхода в рублях API конвертации вызывается всё равно")
        void getUserTotalExpenseInRub_rubCurrency_stillCallsConversionApi() {
            TransactionDto t = new TransactionDto();
            t.setType(TransactionType.EXPENSE);
            t.setAmount(new BigDecimal("100.00"));
            t.setAccountCurrency(Currency.RUB);

            when(currencyApiService.convertCurrency("RUB", "RUB", new BigDecimal("100.00")))
                    .thenReturn(new BigDecimal("100.00"));

            BigDecimal result = userService.getUserTotalExpenseInRub(List.of(t));

            assertThat(result).isEqualByComparingTo("100.00");
            verify(currencyApiService).convertCurrency("RUB", "RUB", new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("если валюта транзакции null — считает её рублёвой")
        void getUserTotalExpenseInRub_nullCurrency_treatsAsRub() {
            TransactionDto t = new TransactionDto();
            t.setType(TransactionType.EXPENSE);
            t.setAmount(new BigDecimal("50.00"));
            t.setAccountCurrency(null);

            when(currencyApiService.convertCurrency("RUB", "RUB", new BigDecimal("50.00")))
                    .thenReturn(new BigDecimal("50.00"));

            BigDecimal result = userService.getUserTotalExpenseInRub(List.of(t));

            assertThat(result).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("не учитывает транзакции с типом INCOME при подсчёте расходов")
        void getUserTotalExpenseInRub_ignoresIncomeTransactions() {
            TransactionDto income = new TransactionDto();
            income.setType(TransactionType.INCOME);
            income.setAmount(new BigDecimal("100.00"));
            income.setAccountCurrency(Currency.RUB);

            BigDecimal result = userService.getUserTotalExpenseInRub(List.of(income));

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(currencyApiService);
        }
    }
}
