package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.mapper.TransactionMapper;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;


    @Transactional
    public TransactionDto findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Undefined transaction"));
        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public void updateTransaction(TransactionDto dto) {
        Transaction oldTx = transactionRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        Account oldAccount = oldTx.getAccount();
        BigDecimal oldAmount = oldTx.getAmount();
        TransactionType oldType = oldTx.getType();

        boolean changed = false;

        if (dto.getAmount() != null && !dto.getAmount().equals(oldAmount)) {
            oldTx.setAmount(dto.getAmount());
            changed = true;
        }
        if (dto.getType() != null && dto.getType() != oldType) {
            oldTx.setType(dto.getType());
            changed = true;
        }
        if (dto.getAccountId() != null && !dto.getAccountId().equals(oldAccount.getId())) {
            Account newAccount = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            oldTx.setAccount(newAccount);
            changed = true;
        }
        Long oldCategoryId = oldTx.getCategory() != null ? oldTx.getCategory().getId() : null;
        if (!Objects.equals(dto.getCategoryId(), oldCategoryId)) {
            Category newCategory = null;
            if (dto.getCategoryId() != null) {
                newCategory = categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            }
            oldTx.setCategory(newCategory);
            changed = true;
        }
        if (dto.getDescription() != null && !dto.getDescription().equals(oldTx.getDescription())) {
            oldTx.setDescription(dto.getDescription());
            changed = true;
        }
        if (dto.getCreatedAt() != null) {
            oldTx.setCreatedAt(dto.getCreatedAt());
            changed = true;
        }
        if (changed) {
            adjustBalance(oldAccount, oldType, oldAmount, false);
            accountRepository.save(oldAccount);

            Account currentAccount = oldTx.getAccount();
            adjustBalance(currentAccount, oldTx.getType(), oldTx.getAmount(), true);
            accountRepository.save(currentAccount);
        }

        transactionRepository.save(oldTx);
    }


    @Transactional
    public TransactionDto createTransaction(TransactionDto dto) {
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Account not found with id: " + dto.getAccountId()
                ));
        Category category = dto.getCategoryId() != null
                ? categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"))
                : null;

        Transaction transaction = new Transaction();
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDescription(dto.getDescription());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setAccount(account);
        transaction.setCategory(category);

        Transaction savedTransaction = transactionRepository.save(transaction);

        if (savedTransaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(savedTransaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(savedTransaction.getAmount()));
        }
        accountRepository.save(account);

        return transactionMapper.toDto(savedTransaction);
    }

    @Transactional
    public TransactionDto getTransactionById(Long id) {
        return transactionMapper.toDto(transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found")));
    }

    @Transactional
    public List<TransactionDto> getUserTransactions(Long userId) {
        List<Transaction> list = transactionRepository.findByUserId(userId);
        return list.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }


    @Transactional
    public List<TransactionDto> findByUserId(Long userId) {
        List<Transaction> list = transactionRepository.findByUserId(userId);
        return list.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TransactionDto> findByAccountId(Long accountId) {
        List<Transaction> list = transactionRepository.findByAccountId(accountId);
        return list.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TransactionDto> getTransactionsByCategory(Long categoryId) {
        List<Transaction> list = transactionRepository.findByCategoryId(categoryId);
        return list.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BigDecimal calculateUserBalance(Long userId) {
        BigDecimal income = transactionRepository.sumAmountByUserIdAndType(userId, TransactionType.INCOME)
                .orElse(BigDecimal.ZERO);

        BigDecimal expense = transactionRepository.sumAmountByUserIdAndType(userId, TransactionType.EXPENSE)
                .orElse(BigDecimal.ZERO);

        return income.subtract(expense);
    }

    @Transactional
    public List<TransactionDto> findRecentByUserId(Long userId, int recents) {
        List<Transaction> list = transactionRepository.findByUserId(userId);
        List<TransactionDto> listDto = new ArrayList<>();

        for (Transaction transaction : list) {
            Account account = transaction.getAccount();

            TransactionDto transactionDto = transactionMapper.toDto(transaction);
            transactionDto.setAccountName(account.getName());
            transactionDto.setAccountCurrency(account.getCurrency());
            listDto.add(transactionDto);
        }
        return listDto;
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        Account account = accountRepository.findById(transaction.getAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));

        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }
        accountRepository.save(account);

        transactionRepository.deleteById(id);
    }

    private void adjustBalance(Account account, TransactionType type, BigDecimal amount, boolean add) {
        int sign = (type == TransactionType.INCOME) ? 1 : -1;
        BigDecimal delta = amount.multiply(BigDecimal.valueOf(sign));
        if (!add) {
            delta = delta.negate();
        }
        account.setBalance(account.getBalance().add(delta));
    }
}
