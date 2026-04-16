package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category save(Category category);

    Optional<Category> findById(Long id);

    @Query("SELECT c FROM Category c ORDER BY c.id ASC")
    List<Category> findAllOrderById();

    Boolean existsByName(String name);

    void delete(Category category);

}
