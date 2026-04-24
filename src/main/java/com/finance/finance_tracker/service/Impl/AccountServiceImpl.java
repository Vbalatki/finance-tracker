package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InsufficientFundsException;
import com.finance.finance_tracker.exception.InvalidAmountException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.AccountMapper;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.Util.DataConstants.USER_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.ACCOUNT_NAME_EXISTS;
import static com.finance.finance_tracker.Util.DataConstants.ACCOUNT_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.INSUFFICIENT_FUNDS;
import static com.finance.finance_tracker.Util.DataConstants.INVALID_AMOUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountDto saveAccount(AccountDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND + ", id: " + dto.getUserId()));

        if (accountRepository.existsByNameAndUserId(dto.getName(), dto.getUserId())) {
            throw new DuplicateEntityException(ACCOUNT_NAME_EXISTS + ": " + dto.getName());
        }

        if (dto.getCurrency() == null) {
            throw new InvalidDataException("Валюта счёта не указана");
        }

        Account account = new Account();
        account.setName(dto.getName());
        account.setBalance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        account.setCurrency(dto.getCurrency());
        account.setUser(user);

        Account savedAccount = accountRepository.save(account);
        log.info("Создан новый счёт: id={}, name={}", savedAccount.getId(), savedAccount.getName());

        return accountMapper.toDto(savedAccount);
    }

    @Transactional
    public AccountDto deposit(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(INVALID_AMOUNT);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + accountId));

        BigDecimal oldBalance = account.getBalance();
        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);

        log.info("Пополнение счёта id={}: сумма={}, старый баланс={}, новый баланс={}",
                accountId, amount, oldBalance, updatedAccount.getBalance());

        return accountMapper.toDto(updatedAccount);
    }

    @Transactional
    public AccountDto withdraw(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(INVALID_AMOUNT);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + accountId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(INSUFFICIENT_FUNDS +
                    ". Баланс: " + account.getBalance() + ", запрошено: " + amount);
        }

        BigDecimal oldBalance = account.getBalance();
        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepository.save(account);

        log.info("Снятие со счёта id={}: сумма={}, старый баланс={}, новый баланс={}",
                accountId, amount, oldBalance, updatedAccount.getBalance());

        return accountMapper.toDto(updatedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getUserAccounts(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
        }

        List<Account> accounts = accountRepository.findByUserId(userId);

        return accounts.stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + id));

        if (transactionRepository.existsByAccountId(id)) {
            throw new InvalidDataException("Нельзя удалить счёт, на котором есть транзакции");
        }

        transactionRepository.deleteByAccountId(id);
        accountRepository.delete(account);

        log.info("Удалён счёт id={}, name={}", id, account.getName());
    }

    @Transactional(readOnly = true)
    public AccountDto findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + id));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
        }

        BigDecimal total = accountRepository.getTotalBalanceByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceInCurrency(Long userId, Currency currency) {
        BigDecimal totalInRub = getTotalBalance(userId);
        return totalInRub;
    }

    @Transactional
    public AccountDto updateAccount(Long id, AccountDto dto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + id));

        if (!account.getName().equals(dto.getName()) &&
                accountRepository.existsByNameAndUserId(dto.getName(), account.getUser().getId())) {
            throw new DuplicateEntityException(ACCOUNT_NAME_EXISTS + ": " + dto.getName());
        }

        if (dto.getCurrency() == null) {
            throw new InvalidDataException("Валюта счёта не указана");
        }

        account.setName(dto.getName());
        account.setCurrency(dto.getCurrency());

        Account updatedAccount = accountRepository.save(account);

        log.info("Обновлён счёт id={}", id);

        return accountMapper.toDto(updatedAccount);
    }
}