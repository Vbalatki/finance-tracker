package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    //save
    Category save(Category category);

    //поиск
    Optional<Category> findById(Long id);
    List<Category> findAll();
    @Query("SELECT c FROM Category c WHERE c.user.id = :userId")
    List<Category> findByUserId(Long userId);


    //delete
    void delete(Category category);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Category c " +
            "JOIN c.transactions t " +
            "JOIN t.account a " +
            "WHERE c.name = :name AND a.user.id = :userId")
    boolean existsByNameAndUserId(String name, Long userId);

    @Query("SELECT DISTINCT c FROM Category c " +
            "WHERE c.id IN (" +
            "   SELECT DISTINCT t.category.id FROM Transaction t " +
            "   JOIN t.account a " +
            "   WHERE a.user.id = :userId" +
            ") " +
            "AND c.budget IS NULL " +
            "ORDER BY c.name")
    List<Category> findByUserIdAndBudgetIsNull(@Param("userId") Long userId);

}
