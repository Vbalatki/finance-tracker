package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Budget save(Budget budget);

    List<Budget> findByUserId(Long userId);

    Optional<Budget> findByUserAndCategory(User user, Category category);

    Boolean existsByUserAndCategory(User user, Category category);

    @Query("SELECT b FROM Budget b JOIN FETCH b.category WHERE b.user = :user")
    List<Budget> findByUserWithCategory(@Param("user") User user);

}
