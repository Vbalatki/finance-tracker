package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Transaction save(Transaction transaction);


    Optional<Transaction> findById(Long id);

    @Query("SELECT DISTINCT t FROM Transaction t " +
            "JOIN FETCH t.account a " +
            "JOIN a.user u " +
            "LEFT JOIN FETCH t.category " +
            "WHERE u.id = :userId " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findByUserId(Long userId);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.account " +
            "LEFT JOIN FETCH t.category " +
            "WHERE t.account.id = :accountId")
    List<Transaction> findByAccountId(Long accountId);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.account " +
            "LEFT JOIN FETCH t.category " +
            "WHERE t.account.id IN :accountIds")
    List<Transaction> findByAccountIdIn(@Param("accountIds") List<Long> accountIds);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.account " +
            "LEFT JOIN FETCH t.category " +
            "WHERE t.account.user.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    default List<Transaction> findRecentByUserId(Long userId, int limit) {
        return findRecentByUserId(userId, PageRequest.of(0, limit));
    }

    void delete(Transaction transaction);

    void deleteById(Long id);

    void deleteByAccountId(Long accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.user.id = :userId AND t.type = :type")
    Optional<BigDecimal> sumAmountByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") TransactionType type
    );


    @Query("SELECT t.category.id, t.amount, a.currency FROM Transaction t " +
            "JOIN t.account a " +
            "WHERE t.category.id IN :categoryIds " +
            "AND t.type = com.finance.finance_tracker.entity.enums.TransactionType.EXPENSE " +
            "AND t.createdAt >= :monthStart AND t.createdAt < :monthEnd")
    List<Object[]> findExpensesByCategoryIdsAndMonth(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);


    @Query("SELECT t FROM Transaction t JOIN FETCH t.account a " +
            "WHERE a.user.id = :userId AND t.createdAt >= :monthStart AND t.createdAt < :monthEnd")
    List<Transaction> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    @Query("SELECT t.externalId FROM Transaction t " +
            "WHERE t.externalSource = :externalSource AND t.externalId IN :externalIds")
    List<String> findExistingExternalIds(
            @Param("externalSource") String externalSource,
            @Param("externalIds") List<String> externalIds);

    boolean existsByExternalSourceAndExternalId(String externalSource, String externalId);
}
