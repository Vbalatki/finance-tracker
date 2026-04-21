package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.entity.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {
    TransactionDto findById(Long id);
    void updateTransaction(TransactionDto dto);
    TransactionDto saveTransaction(TransactionDto dto);
    TransactionDto getTransactionById(Long id);
    List<TransactionDto> getUserTransactions(Long userId);
    List<TransactionDto> findByAccountId(Long accountId);
    List<TransactionDto> findByUserId(Long userId);
    List<TransactionDto> getTransactionsByCategory(Long categoryId);
    BigDecimal calculateUserBalance(Long userId);
    List<TransactionDto> findRecentByUserId(Long userId, int recents);
    void deleteTransaction(Long id);
}
