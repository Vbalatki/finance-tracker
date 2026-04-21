package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto saveCategory(CategoryDto dto);
    CategoryDto getCategoryById(Long id);
    List<CategoryDto> getAllCategories();
    void updateCategory(Long id, String name);
    void deleteCategory(Long id);
}
