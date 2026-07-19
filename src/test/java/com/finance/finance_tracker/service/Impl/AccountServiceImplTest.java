package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InsufficientFundsException;
import com.finance.finance_tracker.exception.InvalidAmountException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.AccountMapper;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link AccountServiceImpl}.
 * Репозитории и маппер замоканы — тестируется только бизнес-логика сервиса.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user;
    private Account account;
    private AccountDto accountDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        account = new Account();
        account.setId(10L);
        account.setName("Основной счет");
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(Currency.RUB);
        account.setUser(user);

        accountDto = new AccountDto();
        accountDto.setName("Основной счет");
        accountDto.setBalance(new BigDecimal("1000.00"));
        accountDto.setCurrency(Currency.RUB);
        accountDto.setUserId(1L);
    }

    @Nested
    @DisplayName("saveAccount")
    class SaveAccount {

        @Test
        @DisplayName("создаёт счёт, когда пользователь существует и имя уникально")
        void saveAccount_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(accountRepository.existsByNameAndUserId("Основной счет", 1L)).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenReturn(account);
            when(accountMapper.toDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.saveAccount(accountDto);

            assertThat(result).isEqualTo(accountDto);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            Account saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Основной счет");
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getBalance()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("подставляет ноль, если баланс в dto не указан")
        void saveAccount_nullBalance_defaultsToZero() {
            accountDto.setBalance(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(accountRepository.existsByNameAndUserId(any(), eq(1L))).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenReturn(account);
            when(accountMapper.toDto(account)).thenReturn(accountDto);

            accountService.saveAccount(accountDto);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не найден")
        void saveAccount_userNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> accountService.saveAccount(accountDto));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает DuplicateEntityException, если имя счёта уже занято")
        void saveAccount_duplicateName_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(accountRepository.existsByNameAndUserId("Основной счет", 1L)).thenReturn(true);

            assertThrows(DuplicateEntityException.class, () -> accountService.saveAccount(accountDto));
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("увеличивает баланс на сумму пополнения")
        void deposit_success() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountMapper.toDto(any(Account.class))).thenReturn(accountDto);

            accountService.deposit(10L, new BigDecimal("500.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("бросает InvalidAmountException для нулевой суммы")
        void deposit_zeroAmount_throws() {
            assertThrows(InvalidAmountException.class, () -> accountService.deposit(10L, BigDecimal.ZERO));
            verify(accountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("бросает InvalidAmountException для отрицательной суммы")
        void deposit_negativeAmount_throws() {
            assertThrows(InvalidAmountException.class, () -> accountService.deposit(10L, new BigDecimal("-10")));
        }

        @Test
        @DisplayName("бросает InvalidAmountException для null суммы")
        void deposit_nullAmount_throws() {
            assertThrows(InvalidAmountException.class, () -> accountService.deposit(10L, null));
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если счёт не найден")
        void deposit_accountNotFound_throws() {
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> accountService.deposit(999L, BigDecimal.TEN));
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("уменьшает баланс на сумму снятия")
        void withdraw_success() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountMapper.toDto(any(Account.class))).thenReturn(accountDto);

            accountService.withdraw(10L, new BigDecimal("300.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("700.00");
        }

        @Test
        @DisplayName("бросает InsufficientFundsException, если баланса не хватает")
        void withdraw_insufficientFunds_throws() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

            assertThrows(InsufficientFundsException.class,
                    () -> accountService.withdraw(10L, new BigDecimal("5000.00")));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает InvalidAmountException для отрицательной суммы")
        void withdraw_negativeAmount_throws() {
            assertThrows(InvalidAmountException.class, () -> accountService.withdraw(10L, new BigDecimal("-1")));
        }

        @Test
        @DisplayName("позволяет снять сумму, равную остатку")
        void withdraw_exactBalance_success() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountMapper.toDto(any(Account.class))).thenReturn(accountDto);

            accountService.withdraw(10L, new BigDecimal("1000.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("getUserAccounts")
    class GetUserAccounts {

        @Test
        @DisplayName("возвращает список счетов пользователя")
        void getUserAccounts_success() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(accountRepository.findByUserId(1L)).thenReturn(List.of(account));
            when(accountMapper.toDto(account)).thenReturn(accountDto);

            List<AccountDto> result = accountService.getUserAccounts(1L);

            assertThat(result).hasSize(1).containsExactly(accountDto);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не существует")
        void getUserAccounts_userNotFound_throws() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> accountService.getUserAccounts(99L));
            verify(accountRepository, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("удаляет счёт вместе со всеми его транзакциями")
        void deleteAccount_success() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

            accountService.deleteAccount(10L);

            verify(transactionRepository).deleteByAccountId(10L);
            verify(accountRepository).delete(account);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если счёт не найден")
        void deleteAccount_notFound_throws() {
            when(accountRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> accountService.deleteAccount(404L));
            verify(transactionRepository, never()).deleteByAccountId(any());
        }
    }

    @Nested
    @DisplayName("findById / getTotalBalance")
    class FindAndBalance {

        @Test
        @DisplayName("findById возвращает dto, если счёт найден")
        void findById_success() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountMapper.toDto(account)).thenReturn(accountDto);

            assertThat(accountService.findById(10L)).isEqualTo(accountDto);
        }

        @Test
        @DisplayName("findById бросает EntityNotFoundException, если счёт не найден")
        void findById_notFound_throws() {
            when(accountRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> accountService.findById(404L));
        }

        @Test
        @DisplayName("getTotalBalance возвращает 0, если сумма из БД равна null")
        void getTotalBalance_null_returnsZero() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(accountRepository.getTotalBalanceByUserId(1L)).thenReturn(null);

            assertThat(accountService.getTotalBalance(1L)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("getTotalBalance бросает EntityNotFoundException для несуществующего пользователя")
        void getTotalBalance_userNotFound_throws() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> accountService.getTotalBalance(99L));
        }
    }

    @Nested
    @DisplayName("updateAccount")
    class UpdateAccount {

        @Test
        @DisplayName("обновляет имя и валюту счёта")
        void updateAccount_success() {
            AccountDto updateDto = new AccountDto();
            updateDto.setName("Новое имя");
            updateDto.setCurrency(Currency.USD);

            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountRepository.existsByNameAndUserId("Новое имя", 1L)).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountMapper.toDto(any(Account.class))).thenReturn(accountDto);

            accountService.updateAccount(10L, updateDto);

            assertThat(account.getName()).isEqualTo("Новое имя");
            assertThat(account.getCurrency()).isEqualTo(Currency.USD);
        }

        @Test
        @DisplayName("бросает DuplicateEntityException при попытке взять занятое имя")
        void updateAccount_duplicateName_throws() {
            AccountDto updateDto = new AccountDto();
            updateDto.setName("Занятое имя");
            updateDto.setCurrency(Currency.USD);

            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountRepository.existsByNameAndUserId("Занятое имя", 1L)).thenReturn(true);

            assertThrows(DuplicateEntityException.class, () -> accountService.updateAccount(10L, updateDto));
        }

        @Test
        @DisplayName("не проверяет уникальность имени, если оно не меняется")
        void updateAccount_sameName_skipsUniquenessCheck() {
            AccountDto updateDto = new AccountDto();
            updateDto.setName("Основной счет");
            updateDto.setCurrency(Currency.EUR);

            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountMapper.toDto(any(Account.class))).thenReturn(accountDto);

            accountService.updateAccount(10L, updateDto);

            verify(accountRepository, never()).existsByNameAndUserId(any(), any());
            assertThat(account.getCurrency()).isEqualTo(Currency.EUR);
        }

        @Test
        @DisplayName("бросает InvalidDataException, если валюта не указана")
        void updateAccount_nullCurrency_throws() {
            AccountDto updateDto = new AccountDto();
            updateDto.setName("Основной счет");
            updateDto.setCurrency(null);

            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

            assertThrows(InvalidDataException.class, () -> accountService.updateAccount(10L, updateDto));
        }
    }
}
