package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.RecurringCommitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecurringCommitmentRepository extends JpaRepository<RecurringCommitment, Long> {

    // LEFT JOIN FETCH r.account добавлен вместе с r.category — тот же класс
    // бага, что уже чинили в Budget/Transaction/BankImport: RecurringCommitmentMapper.toDto
    // читает account.id/account.name для каждой строки (см. @Mapping в мапперe),
    // а без фетча это был отдельный SELECT по account на каждый плановый платёж.
    // LEFT, не INNER — account у RecurringCommitment может быть null (см. entity:
    // @ManyToOne без nullable=false на account, в отличие от user).
    @Query("SELECT r FROM RecurringCommitment r " +
            "LEFT JOIN FETCH r.category " +
            "LEFT JOIN FETCH r.account " +
            "WHERE r.user.id = :userId ORDER BY r.dayOfMonth ASC")
    List<RecurringCommitment> findByUserIdOrderByDayOfMonth(@Param("userId") Long userId);

    // account здесь фактически не читается (SpendingCalendarServiceImpl использует
    // только name/amount/type/dayOfMonth), но фетч добавлен для единообразия с
    // findByUserIdOrderByDayOfMonth — цена лишнего LEFT JOIN в одном запросе
    // пренебрежимо мала, а рассинхрон между двумя похожими методами репозитория
    // сам по себе источник будущих ошибок (кто-то скопирует not-fetching вариант
    // туда, где account всё-таки нужен).
    @Query("SELECT r FROM RecurringCommitment r " +
            "LEFT JOIN FETCH r.category " +
            "LEFT JOIN FETCH r.account " +
            "WHERE r.user.id = :userId AND r.active = true")
    List<RecurringCommitment> findActiveByUserId(@Param("userId") Long userId);
}