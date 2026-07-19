package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidAmountException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.TransactionMapper;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link TransactionServiceImpl}.
 * Основной фокус — корректность пересчёта баланса счёта при
 * создании / обновлении / удалении транзакций.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account account;
    private TransactionDto transactionDto;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(10L);
        account.setName("Основной счет");
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(Currency.RUB);

        transactionDto = new TransactionDto();
        transactionDto.setAccountId(10L);
        transactionDto.setAmount(new BigDecimal("200.00"));
        transactionDto.setType(TransactionType.INCOME);
    }

    @Nested
    @DisplayName("saveTransaction")
    class SaveTransaction {

        @Test
        @DisplayName("поступление увеличивает баланс счёта")
        void saveTransaction_income_increasesBalance() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(transactionMapper.toDto(any(Transaction.class))).thenReturn(transactionDto);

            transactionService.saveTransaction(transactionDto);

            assertThat(account.getBalance()).isEqualByComparingTo("1200.00");
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("расход уменьшает баланс счёта")
        void saveTransaction_expense_decreasesBalance() {
            transactionDto.setType(TransactionType.EXPENSE);
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(transactionMapper.toDto(any(Transaction.class))).thenReturn(transactionDto);

            transactionService.saveTransaction(transactionDto);

            assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        }

        @Test
        @DisplayName("подставляет категорию, если categoryId указан")
        void saveTransaction_withCategory_setsCategory() {
            Category category = new Category();
            category.setId(5L);
            category.setName("Продукты");
            transactionDto.setCategoryId(5L);

            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                assertThat(t.getCategory()).isEqualTo(category);
                return t;
            });
            when(transactionMapper.toDto(any(Transaction.class))).thenReturn(transactionDto);

            transactionService.saveTransaction(transactionDto);

            verify(categoryRepository).findById(5L);
        }

        @Test
        @DisplayName("бросает InvalidDataException, если accountId не указан")
        void saveTransaction_accountIdNull_throws() {
            transactionDto.setAccountId(null);

            assertThrows(InvalidDataException.class, () -> transactionService.saveTransaction(transactionDto));
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает InvalidAmountException для нулевой суммы")
        void saveTransaction_zeroAmount_throws() {
            transactionDto.setAmount(BigDecimal.ZERO);

            assertThrows(InvalidAmountException.class, () -> transactionService.saveTransaction(transactionDto));
        }

        @Test
        @DisplayName("бросает InvalidAmountException для null суммы")
        void saveTransaction_nullAmount_throws() {
            transactionDto.setAmount(null);

            assertThrows(InvalidAmountException.class, () -> transactionService.saveTransaction(transactionDto));
        }

        @Test
        @DisplayName("бросает InvalidDataException, если тип транзакции не указан")
        void saveTransaction_typeNull_throws() {
            transactionDto.setType(null);

            assertThrows(InvalidDataException.class, () -> transactionService.saveTransaction(transactionDto));
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если счёт не найден")
        void saveTransaction_accountNotFound_throws() {
            when(accountRepository.findById(10L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> transactionService.saveTransaction(transactionDto));
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если категория не найдена")
        void saveTransaction_categoryNotFound_throws() {
            transactionDto.setCategoryId(99L);
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> transactionService.saveTransaction(transactionDto));
        }
    }

    @Nested
    @DisplayName("updateTransaction")
    class UpdateTransaction {

        @Test
        @DisplayName("при изменении суммы корректирует баланс на разницу")
        void updateTransaction_amountChanged_adjustsBalance() {
            account.setBalance(new BigDecimal("500.00"));
            Transaction existing = new Transaction();
            existing.setId(1L);
            existing.setAmount(new BigDecimal("100.00"));
            existing.setType(TransactionType.INCOME);
            existing.setAccount(account);

            TransactionDto dto = new TransactionDto();
            dto.setId(1L);
            dto.setAmount(new BigDecimal("300.00"));

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(existing);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.updateTransaction(dto);

            // было +100 в балансе, стало +300 => чистое изменение +200
            assertThat(account.getBalance()).isEqualByComparingTo("700.00");
        }

        @Test
        @DisplayName("при смене типа с INCOME на EXPENSE баланс корректируется дважды")
        void updateTransaction_typeChanged_adjustsBalance() {
            account.setBalance(new BigDecimal("500.00"));
            Transaction existing = new Transaction();
            existing.setId(1L);
            existing.setAmount(new BigDecimal("100.00"));
            existing.setType(TransactionType.INCOME);
            existing.setAccount(account);

            TransactionDto dto = new TransactionDto();
            dto.setId(1L);
            dto.setType(TransactionType.EXPENSE);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(existing);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.updateTransaction(dto);

            // отмена старого +100 (-100), затем применение нового как расхода (-100) => -200 от исходных 500
            assertThat(account.getBalance()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("при смене счёта корректирует баланс обоих счетов")
        void updateTransaction_accountChanged_adjustsBothAccounts() {
            account.setBalance(new BigDecimal("500.00"));
            Account newAccount = new Account();
            newAccount.setId(20L);
            newAccount.setBalance(new BigDecimal("200.00"));
            newAccount.setCurrency(Currency.RUB);

            Transaction existing = new Transaction();
            existing.setId(1L);
            existing.setAmount(new BigDecimal("100.00"));
            existing.setType(TransactionType.INCOME);
            existing.setAccount(account);

            TransactionDto dto = new TransactionDto();
            dto.setId(1L);
            dto.setAccountId(20L);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(accountRepository.findById(20L)).thenReturn(Optional.of(newAccount));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(existing);
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.updateTransaction(dto);

            assertThat(account.getBalance()).isEqualByComparingTo("400.00");
            assertThat(newAccount.getBalance()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("если поля не изменились — баланс не пересчитывается")
        void updateTransaction_noChanges_doesNotTouchBalance() {
            Transaction existing = new Transaction();
            existing.setId(1L);
            existing.setAmount(new BigDecimal("100.00"));
            existing.setType(TransactionType.INCOME);
            existing.setAccount(account);

            TransactionDto dto = new TransactionDto();
            dto.setId(1L);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(existing);

            transactionService.updateTransaction(dto);

            verify(accountRepository, never()).save(any());
            assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если транзакция не найдена")
        void updateTransaction_notFound_throws() {
            TransactionDto dto = new TransactionDto();
            dto.setId(404L);

            when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> transactionService.updateTransaction(dto));
        }
    }

    @Nested
    @DisplayName("deleteTransaction")
    class DeleteTransaction {

        @Test
        @DisplayName("удаление дохода уменьшает баланс счёта")
        void deleteTransaction_income_decreasesBalance() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setAmount(new BigDecimal("100.00"));
            tx.setType(TransactionType.INCOME);
            tx.setAccount(account);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.deleteTransaction(1L);

            assertThat(account.getBalance()).isEqualByComparingTo("900.00");
            verify(transactionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("удаление расхода увеличивает баланс счёта")
        void deleteTransaction_expense_increasesBalance() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setAmount(new BigDecimal("100.00"));
            tx.setType(TransactionType.EXPENSE);
            tx.setAccount(account);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.deleteTransaction(1L);

            assertThat(account.getBalance()).isEqualByComparingTo("1100.00");
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если транзакция не найдена")
        void deleteTransaction_notFound_throws() {
            when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> transactionService.deleteTransaction(404L));
        }

        @Test
        @DisplayName("бросает InvalidDataException, если у транзакции нет счёта")
        void deleteTransaction_noAccount_throws() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setAmount(new BigDecimal("100.00"));
            tx.setType(TransactionType.INCOME);
            tx.setAccount(null);

            when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

            assertThrows(InvalidDataException.class, () -> transactionService.deleteTransaction(1L));
            verify(transactionRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("calculateUserBalance / findRecentByUserId")
    class BalanceAndRecent {

        @Test
        @DisplayName("считает баланс как доходы минус расходы")
        void calculateUserBalance_success() {
            when(transactionRepository.sumAmountByUserIdAndType(1L, TransactionType.INCOME))
                    .thenReturn(Optional.of(new BigDecimal("300.00")));
            when(transactionRepository.sumAmountByUserIdAndType(1L, TransactionType.EXPENSE))
                    .thenReturn(Optional.of(new BigDecimal("100.00")));

            BigDecimal result = transactionService.calculateUserBalance(1L);

            assertThat(result).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("если сумм в БД нет — считает их нулевыми")
        void calculateUserBalance_emptySums_defaultsToZero() {
            when(transactionRepository.sumAmountByUserIdAndType(1L, TransactionType.INCOME))
                    .thenReturn(Optional.empty());
            when(transactionRepository.sumAmountByUserIdAndType(1L, TransactionType.EXPENSE))
                    .thenReturn(Optional.empty());

            BigDecimal result = transactionService.calculateUserBalance(1L);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("подставляет имя и валюту счёта в последние транзакции")
        void findRecentByUserId_setsAccountNameAndCurrency() {
            Transaction tx = new Transaction();
            tx.setId(1L);
            tx.setAmount(new BigDecimal("50.00"));
            tx.setType(TransactionType.EXPENSE);
            tx.setAccount(account);

            TransactionDto dto = new TransactionDto();
            when(transactionRepository.findRecentByUserId(1L, 5)).thenReturn(List.of(tx));
            when(transactionMapper.toDto(tx)).thenReturn(dto);

            List<TransactionDto> result = transactionService.findRecentByUserId(1L, 5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAccountName()).isEqualTo("Основной счет");
            assertThat(result.get(0).getAccountCurrency()).isEqualTo(Currency.RUB);
        }
    }
}
