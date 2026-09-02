package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.BankConnector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankImportServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BankConnector tBankConnector;

    @InjectMocks
    private BankImportServiceImpl bankImportService;

    @Test
    @DisplayName("syncTransactions пропускает уже импортированные операции по externalId")
    void syncTransactions_skipsAlreadyImported() {
        User user = new User();
        user.setEmail("user@example.com");

        Account account = new Account();
        account.setId(10L);
        account.setBankCode("TBANK");
        account.setExternalAccountNumber("40702810110011000000");
        account.setUser(user);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        BankTransactionDto tx = new BankTransactionDto(
                "op-1", "40702810110011000000", new BigDecimal("-500.00"), "RUB", "Магазин", LocalDateTime.now());

        when(tBankConnector.fetchTransactions(eq("40702810110011000000"), any(), any(), eq("user@example.com")))
                .thenReturn(new BankStatementResult(List.of(tx), null));
        when(transactionRepository.findExistingExternalIds(eq("TBANK"), eq(List.of("op-1"))))
                .thenReturn(List.of("op-1"));

        int imported = bankImportService.syncTransactions(10L, LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(imported).isEqualTo(0);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("бросает InvalidDataException для счёта без привязки к банку")
    void syncTransactions_unlinkedAccount_throws() {
        Account account = new Account();
        account.setId(10L);
        // bankCode не установлен

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThrows(InvalidDataException.class,
                () -> bankImportService.syncTransactions(10L, LocalDateTime.now().minusDays(30), LocalDateTime.now()));
    }

    @Test
    @DisplayName("syncTransactions импортирует новую операцию, которой ещё нет в БД")
    void syncTransactions_newOperation_isSaved() {
        User user = new User();
        user.setEmail("user@example.com");

        Account account = new Account();
        account.setId(10L);
        account.setBankCode("TBANK");
        account.setExternalAccountNumber("40702810110011000000");
        account.setUser(user);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        BankTransactionDto tx = new BankTransactionDto(
                "op-2", "40702810110011000000", new BigDecimal("1000.00"), "RUB", "Зарплата", LocalDateTime.now());

        when(tBankConnector.fetchTransactions(eq("40702810110011000000"), any(), any(), eq("user@example.com")))
                .thenReturn(new BankStatementResult(List.of(tx), null));
        when(transactionRepository.findExistingExternalIds(eq("TBANK"), eq(List.of("op-2"))))
                .thenReturn(List.of());

        int imported = bankImportService.syncTransactions(10L, LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(imported).isEqualTo(1);
        verify(transactionRepository).saveAll(argThat(list -> {
            List<?> txs = (List<?>) list;
            return txs.size() == 1;
        }));
    }

    @Test
    @DisplayName("syncTransactions дедуплицирует операции с одинаковым externalId внутри одного ответа банка")
    void syncTransactions_duplicateExternalIdWithinSameStatement_savesOnlyOnce() {
        User user = new User();
        user.setEmail("user@example.com");

        Account account = new Account();
        account.setId(10L);
        account.setBankCode("TBANK");
        account.setExternalAccountNumber("40702810110011000000");
        account.setUser(user);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        BankTransactionDto tx1 = new BankTransactionDto(
                "op-dup", "40702810110011000000", new BigDecimal("500.00"), "RUB", "Зарплата", LocalDateTime.now());
        BankTransactionDto tx2 = new BankTransactionDto(
                "op-dup", "40702810110011000000", new BigDecimal("500.00"), "RUB", "Зарплата", LocalDateTime.now());

        when(tBankConnector.fetchTransactions(eq("40702810110011000000"), any(), any(), eq("user@example.com")))
                .thenReturn(new BankStatementResult(List.of(tx1, tx2), null));
        when(transactionRepository.findExistingExternalIds(eq("TBANK"), eq(List.of("op-dup"))))
                .thenReturn(List.of());

        int imported = bankImportService.syncTransactions(10L, LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(imported).isEqualTo(1);
        verify(transactionRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }

    @Test
    @DisplayName("linkAccount бросает InvalidDataException для неизвестного bankCode")
    void linkAccount_unknownBankCode_throws() {
        assertThrows(InvalidDataException.class,
                () -> bankImportService.linkAccount(1L, "SBER", "40702810000000000001",
                        "Сбер счёт", com.finance.finance_tracker.entity.enums.Currency.RUB));

        verifyNoInteractions(userRepository, accountRepository);
    }

    @Test
    @DisplayName("linkAccount бросает InvalidDataException для null bankCode")
    void linkAccount_nullBankCode_throws() {
        assertThrows(InvalidDataException.class,
                () -> bankImportService.linkAccount(1L, null, "40702810000000000001",
                        "Счёт", com.finance.finance_tracker.entity.enums.Currency.RUB));
    }
}