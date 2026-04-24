package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidAmountException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.TransactionMapper;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.Util.DataConstants.ACCOUNT_ID_REQUIRED;
import static com.finance.finance_tracker.Util.DataConstants.ACCOUNT_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.CATEGORY_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.INVALID_AMOUNT;
import static com.finance.finance_tracker.Util.DataConstants.TRANSACTION_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public TransactionDto findById(Long id) {
        log.debug("Поиск транзакции по id: {}", id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Транзакция не найдена: id={}", id);
                    return new EntityNotFoundException(TRANSACTION_NOT_FOUND + ", id: " + id);
                });
        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public void updateTransaction(TransactionDto dto) {
        log.debug("Обновление транзакции: id={}", dto.getId());

        Transaction oldTx = transactionRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    log.error("Транзакция не найдена при обновлении: id={}", dto.getId());
                    return new EntityNotFoundException(TRANSACTION_NOT_FOUND + ", id: " + dto.getId());
                });

        Account oldAccount = oldTx.getAccount();
        BigDecimal oldAmount = oldTx.getAmount();
        TransactionType oldType = oldTx.getType();

        boolean changed = false;

        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(INVALID_AMOUNT);
        }

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
                    .orElseThrow(() -> {
                        log.error("Счёт не найден при обновлении транзакции: accountId={}", dto.getAccountId());
                        return new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + dto.getAccountId());
                    });
            oldTx.setAccount(newAccount);
            changed = true;
        }
        Long oldCategoryId = oldTx.getCategory() != null ? oldTx.getCategory().getId() : null;
        if (!Objects.equals(dto.getCategoryId(), oldCategoryId)) {
            Category newCategory = null;
            if (dto.getCategoryId() != null) {
                newCategory = categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(() -> {
                            log.error("Категория не найдена при обновлении транзакции: categoryId={}", dto.getCategoryId());
                            return new EntityNotFoundException(CATEGORY_NOT_FOUND + ", id: " + dto.getCategoryId());
                        });
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

            log.info("Обновлена транзакция: id={}, oldAmount={}, newAmount={}, oldType={}, newType={}",
                    dto.getId(), oldAmount, oldTx.getAmount(), oldType, oldTx.getType());
        }

        transactionRepository.save(oldTx);
    }

    @Transactional
    public TransactionDto saveTransaction(TransactionDto dto) {
        log.debug("Сохранение новой транзакции: accountId={}, type={}, amount={}",
                dto.getAccountId(), dto.getType(), dto.getAmount());

        if (dto.getAccountId() == null) {
            throw new InvalidDataException(ACCOUNT_ID_REQUIRED);
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(INVALID_AMOUNT);
        }
        if (dto.getType() == null) {
            throw new InvalidDataException("Тип транзакции обязателен");
        }

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> {
                    log.error("Счёт не найден при сохранении транзакции: accountId={}", dto.getAccountId());
                    return new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + dto.getAccountId());
                });

        Category category = dto.getCategoryId() != null
                ? categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Категория не найдена при сохранении транзакции: categoryId={}", dto.getCategoryId());
                    return new EntityNotFoundException(CATEGORY_NOT_FOUND + ", id: " + dto.getCategoryId());
                })
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

        log.info("Создана новая транзакция: id={}, type={}, amount={}, accountId={}",
                savedTransaction.getId(), savedTransaction.getType(), savedTransaction.getAmount(), account.getId());

        return transactionMapper.toDto(savedTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long id) {
        log.debug("Поиск транзакции по id: {}", id);
        return transactionMapper.toDto(transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Транзакция не найдена: id={}", id);
                    return new EntityNotFoundException(TRANSACTION_NOT_FOUND + ", id: " + id);
                }));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getUserTransactions(Long userId) {
        log.debug("Запрос транзакций для пользователя: userId={}", userId);
        List<Transaction> list = transactionRepository.findByUserId(userId);
        log.debug("Найдено транзакций: {}", list.size());
        return list.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> findByUserId(Long userId) {
        return getUserTransactions(userId);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> findByAccountId(Long accountId) {
        log.debug("Запрос транзакций по счёту: accountId={}", accountId);
        List<Transaction> list = transactionRepository.findByAccountId(accountId);
        log.debug("Найдено транзакций: {}", list.size());
        return list.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByCategory(Long categoryId) {
        log.debug("Запрос транзакций по категории: categoryId={}", categoryId);
        List<Transaction> list = transactionRepository.findByCategoryId(categoryId);
        log.debug("Найдено транзакций: {}", list.size());
        return list.stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateUserBalance(Long userId) {
        log.debug("Расчёт баланса пользователя: userId={}", userId);
        BigDecimal income = transactionRepository.sumAmountByUserIdAndType(userId, TransactionType.INCOME)
                .orElse(BigDecimal.ZERO);
        BigDecimal expense = transactionRepository.sumAmountByUserIdAndType(userId, TransactionType.EXPENSE)
                .orElse(BigDecimal.ZERO);
        BigDecimal balance = income.subtract(expense);
        log.debug("Баланс пользователя {}: доход={}, расход={}, баланс={}", userId, income, expense, balance);
        return balance;
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> findRecentByUserId(Long userId, int limit) {
        log.debug("Запрос последних {} транзакций пользователя: userId={}", limit, userId);
        List<Transaction> transactions = transactionRepository.findRecentByUserId(userId, limit);
        return transactions.stream()
                .map(transaction -> {
                    TransactionDto dto = transactionMapper.toDto(transaction);
                    Account account = transaction.getAccount();
                    if (account != null) {
                        dto.setAccountName(account.getName());
                        dto.setAccountCurrency(account.getCurrency());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTransaction(Long id) {
        log.debug("Удаление транзакции: id={}", id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Транзакция не найдена при удалении: id={}", id);
                    return new EntityNotFoundException(TRANSACTION_NOT_FOUND + ", id: " + id);
                });

        Account account = transaction.getAccount();
        if (account == null) {
            log.error("У транзакции {} отсутствует счёт", id);
            throw new InvalidDataException("У транзакции отсутствует счёт");
        }

        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }
        accountRepository.save(account);

        transactionRepository.deleteById(id);
        log.info("Удалена транзакция: id={}, type={}, amount={}", id, transaction.getType(), transaction.getAmount());
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