package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.dto.bank.BankStatementResult;
import com.finance.finance_tracker.dto.bank.TBankOperationDto;
import com.finance.finance_tracker.dto.bank.BankTransactionDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.BankConnector;
import com.finance.finance_tracker.service.BankImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.util.DataConstants.ACCOUNT_NAME_EXISTS;
import static com.finance.finance_tracker.util.DataConstants.ACCOUNT_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankImportServiceImpl implements BankImportService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    private final BankConnector tBankConnector;

    @Override
    @Transactional
    public Long linkAccount(Long userId, String bankCode, String externalAccountNumber,
                            String accountName, Currency currency) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId));

        if (accountRepository.existsByBankCodeAndExternalAccountNumber(bankCode, externalAccountNumber)) {
            throw new DuplicateEntityException(
                    "Счёт " + externalAccountNumber + " в банке " + bankCode + " уже привязан");
        }
        if (accountRepository.existsByNameAndUserId(accountName, userId)) {
            throw new DuplicateEntityException(ACCOUNT_NAME_EXISTS + ": " + accountName);
        }

        Account account = new Account();
        account.setName(accountName);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        account.setBankCode(bankCode);
        account.setExternalAccountNumber(externalAccountNumber);

        Account saved = accountRepository.save(account);
        log.info("Привязан банковский счёт: accountId={}, bankCode={}, externalAccountNumber={}",
                saved.getId(), bankCode, externalAccountNumber);

        return saved.getId();
    }

    @Override
    @Transactional
    public int syncTransactions(Long accountId, LocalDateTime from, LocalDateTime to) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + accountId));

        if (account.getBankCode() == null || account.getExternalAccountNumber() == null) {
            throw new InvalidDataException("Счёт id=" + accountId + " не привязан ни к какому банку");
        }

        BankConnector connector = resolveConnector(account.getBankCode());
        BankStatementResult statement =
                connector.fetchTransactions(account.getExternalAccountNumber(), from, to);


        List<String> externalIds = statement.transactions().stream()
                .map(BankTransactionDto::externalId)
                .collect(Collectors.toList());

        Set<String> alreadyImported = externalIds.isEmpty()
                ? Set.of()
                : new HashSet<>(transactionRepository.findExistingExternalIds(account.getBankCode(), externalIds));

        List<Transaction> newTransactions = new ArrayList<>();
        for (BankTransactionDto bankTx : statement.transactions()) {
            if (alreadyImported.contains(bankTx.externalId())) {
                continue;
            }

            Transaction tx = new Transaction();
            tx.setAccount(account);
            tx.setAmount(bankTx.amount().abs());
            tx.setType(bankTx.amount().signum() >= 0 ? TransactionType.INCOME : TransactionType.EXPENSE);
            tx.setDescription(bankTx.description());
            tx.setCreatedAt(bankTx.bookingDate());
            tx.setExternalSource(account.getBankCode());
            tx.setExternalId(bankTx.externalId());
            newTransactions.add(tx);
        }

        transactionRepository.saveAll(newTransactions);
        int imported = newTransactions.size();

        if (statement.endingBalance() != null && !to.isBefore(LocalDateTime.now().minusMinutes(5))) {
            account.setBalance(statement.endingBalance());
            log.info("Баланс счёта id={} обновлён из выписки банка: {}", accountId, statement.endingBalance());
        }

        account.setLastSyncedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("Синхронизация счёта id={}: получено {} операций, импортировано {} новых",
                accountId, statement.transactions().size(), imported);

        return imported;
    }

    private BankConnector resolveConnector(String bankCode) {
        if (!"TBANK".equals(bankCode)) {
            throw new InvalidDataException("Неизвестный банк: " + bankCode);
        }
        return tBankConnector;
    }
}