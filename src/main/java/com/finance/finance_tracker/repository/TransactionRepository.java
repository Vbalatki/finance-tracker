package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.TransactionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Transaction save(Transaction transaction);

    boolean existsByAccountId(Long id);

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

    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    default List<Transaction> findRecentByUserId(Long userId, int limit) {
        return findRecentByUserId(userId, PageRequest.of(0, limit));
    }

    void delete(Transaction transaction);

    void deleteById(Long id);

    void deleteByAccountId(Long accountId);

    @Query("DELETE FROM Transaction t WHERE t.account.user.id = :userId AND t.category.id = :categoryId")
    void deleteByUserIdAndCategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.account.id = :accountId AND t.type = :type")
    Optional<BigDecimal> sumAmountByUserIdAndType(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type
    );

    /**
     * Сумма расходов по категории за произвольный период (обычно — текущий месяц,
     * границы вычисляются в BudgetServiceImpl через {@code LocalDate.now()}).
     *
     * <p>Раньше здесь использовались функции {@code YEAR()}/{@code MONTH()},
     * специфичные для MySQL — на PostgreSQL таких JPQL-функций нет. Заменено на
     * сравнение диапазона дат, что работает одинаково на любой СУБД и вдобавок
     * не зависит от часового пояса сервера БД.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.category.id = :categoryId " +
            "AND t.type = com.finance.finance_tracker.entity.enums.TransactionType.EXPENSE " +
            "AND t.createdAt >= :monthStart AND t.createdAt < :monthEnd")
    BigDecimal getCurrentMonthExpenseByCategory(
            @Param("categoryId") Long categoryId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);
}
