package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto dto);
    CategoryDto getCategoryById(Long id);
    List<CategoryDto> getAllCategories();
    List<CategoryDto> getUserCategories(Long userId);
    List<CategoryDto> findWithoutBudget(Long userId);
    void deleteCategory(Long id);
}
