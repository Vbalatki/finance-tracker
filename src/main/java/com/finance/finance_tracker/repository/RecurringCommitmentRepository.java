package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.RecurringCommitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecurringCommitmentRepository extends JpaRepository<RecurringCommitment, Long> {

    @Query("SELECT r FROM RecurringCommitment r LEFT JOIN FETCH r.category WHERE r.user.id = :userId ORDER BY r.dayOfMonth ASC")
    List<RecurringCommitment> findByUserIdOrderByDayOfMonth(@Param("userId") Long userId);

    @Query("SELECT r FROM RecurringCommitment r LEFT JOIN FETCH r.category WHERE r.user.id = :userId AND r.active = true")
    List<RecurringCommitment> findActiveByUserId(@Param("userId") Long userId);
}