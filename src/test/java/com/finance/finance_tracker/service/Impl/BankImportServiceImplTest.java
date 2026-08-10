package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Transaction;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        Account account = new Account();
        account.setId(10L);
        account.setBankCode("TBANK");
        account.setExternalAccountNumber("40702810110011000000");

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        BankTransactionDto tx = new BankTransactionDto(
                "op-1", "40702810110011000000", new BigDecimal("-500.00"), "RUB", "Магазин", LocalDateTime.now());

        when(tBankConnector.fetchTransactions(eq("40702810110011000000"), any(), any()))
                .thenReturn(new BankStatementResult(List.of(tx), null));
        when(transactionRepository.findExistingExternalIds(eq("TBANK"), eq(List.of("op-1"))))
                .thenReturn(List.of("op-1"));

        int imported = bankImportService.syncTransactions(10L, LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(imported).isEqualTo(0);
        verify(transactionRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("syncTransactions импортирует новую операцию, которой ещё нет в БД")
    void syncTransactions_newOperation_isSaved() {
        Account account = new Account();
        account.setId(10L);
        account.setBankCode("TBANK");
        account.setExternalAccountNumber("40702810110011000000");

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        BankTransactionDto tx = new BankTransactionDto(
                "op-2", "40702810110011000000", new BigDecimal("1000.00"), "RUB", "Зарплата", LocalDateTime.now());

        when(tBankConnector.fetchTransactions(eq("40702810110011000000"), any(), any()))
                .thenReturn(new BankStatementResult(List.of(tx), null));
        when(transactionRepository.findExistingExternalIds(eq("TBANK"), eq(List.of("op-2"))))
                .thenReturn(List.of());

        int imported = bankImportService.syncTransactions(10L, LocalDateTime.now().minusDays(30), LocalDateTime.now());

        assertThat(imported).isEqualTo(1);
        verify(transactionRepository).saveAll(argThat(list -> {
            List<Transaction> txs = (List<Transaction>) list;
            return txs.size() == 1 && "op-2".equals(txs.get(0).getExternalId());
        }));
    }

    @Test
    @DisplayName("бросает InvalidDataException для счёта без привязки к банку")
    void syncTransactions_unlinkedAccount_throws() {
        Account account = new Account();
        account.setId(10L);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThrows(InvalidDataException.class,
                () -> bankImportService.syncTransactions(10L, LocalDateTime.now().minusDays(30), LocalDateTime.now()));
    }
}
