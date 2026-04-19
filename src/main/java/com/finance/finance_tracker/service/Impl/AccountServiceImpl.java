package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.mapper.AccountMapper;
import com.finance.finance_tracker.mapper.UserMapper;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.AccountService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.Util.DataConstants.USER_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.ACCOUNT_NAME_EXISTS;
import static com.finance.finance_tracker.Util.DataConstants.ACCOUNT_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.INSUFFICIENT_FUNDS;
import static com.finance.finance_tracker.Util.DataConstants.CANNOT_DELETE_ACCOUNT;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountDto createAccount(AccountDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        if (accountRepository.existsByNameAndUserId(dto.getName(), dto.getUserId())) {
            throw new IllegalArgumentException(ACCOUNT_NAME_EXISTS);
        }

        // Создаем счет
        Account account = new Account();
        account.setName(dto.getName());
        account.setBalance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        account.setCurrency(dto.getCurrency());
        account.setUser(user);

        // Сохраняем
        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDto(savedAccount);
    }

    @Transactional
    public AccountDto deposit(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть положительной");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND));

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);
        return accountMapper.toDto(updatedAccount);
    }

    @Transactional
    public AccountDto withdraw(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма снятия должна быть положительной");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(INSUFFICIENT_FUNDS);
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepository.save(account);
        return accountMapper.toDto(updatedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getUserAccounts(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);

        return accounts.stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND));

        transactionRepository.deleteByAccountId(id);
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public AccountDto findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance(Long userId) {
        return accountRepository.getTotalBalanceByUserId(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceInCurrency(Long userId, Currency currency) {
        // Здесь нужна логика конвертации валют
        return accountRepository.getTotalBalanceByUserId(userId);
    }

    @Transactional
    public AccountDto updateAccount(Long id, AccountDto dto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND));

        // Проверяем уникальность имени, если оно изменилось
        if (!account.getName().equals(dto.getName()) &&
                accountRepository.existsByNameAndUserId(dto.getName(), account.getUser().getId())) {
            throw new IllegalArgumentException(ACCOUNT_NAME_EXISTS);
        }

        account.setName(dto.getName());
        account.setCurrency(dto.getCurrency());

        Account updatedAccount = accountRepository.save(account);
        return accountMapper.toDto(updatedAccount);
    }
}