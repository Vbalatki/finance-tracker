package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category save(Category category);

    Optional<Category> findById(Long id);

    @Query("SELECT c FROM Category c ORDER BY c.id ASC")
    List<Category> findAllOrderById();

    @Query("SELECT c FROM Category c WHERE c.user.id = :userId OR c.user IS NULL ORDER BY c.id ASC")
    List<Category> findVisibleToUserOrderByIdAsc(@Param("userId") Long userId);

    List<Category> findByUserId(Long userId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND (c.user.id = :userId OR c.user IS NULL)")
    boolean existsByNameVisibleToUser(@Param("name") String name, @Param("userId") Long userId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND (c.user.id = :userId OR c.user IS NULL) AND c.id <> :id")
    boolean existsByNameVisibleToUserAndIdNot(@Param("name") String name, @Param("userId") Long userId, @Param("id") Long id);

    void delete(Category category);
}
