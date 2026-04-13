package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    //create
    Budget save(Budget budget);

    //read
    Optional<Budget> findById(Long id);
    Optional<Budget> findByCategory(Category category);

    @Query("SELECT DISTINCT b FROM Budget b " +
            "JOIN b.category c " +
            "JOIN c.transactions t " +
            "JOIN t.account a " +
            "JOIN a.user u " +
            "WHERE u.id = :userId")
    List<Budget> findByUserId(Long userId);
    //delete
    void deleteById(Long id);

    //exists
    boolean existsByCategory(Category category);
}
