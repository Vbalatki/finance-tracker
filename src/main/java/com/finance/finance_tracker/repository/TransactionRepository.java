package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    //save
    Transaction save(Transaction transaction);

    //find methods
    Optional<Transaction> findById(Long id);

    @Query("SELECT DISTINCT t FROM Transaction t " +
            "JOIN t.account a " +
            "JOIN a.user u " +
            "WHERE u.id = :userId " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findByUserId(Long userId);

    @Query("SELECT t FROM Transaction t JOIN t.category c WHERE c.id = :categoryId")
    List<Transaction> findByCategoryId(Long categoryId);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.account " +
            "LEFT JOIN FETCH t.category " +
            "WHERE t.account.id = :accountId")
    List<Transaction> findByAccountId(Long accountId);

    //delete
    void deleteById(Long id);

    //Custom
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.id = :userId AND t.type = :type")
    Optional<BigDecimal> sumAmountByUserIdAndType(
            @Param("account_id") Long userId,
            @Param("type") TransactionType type
    );
}