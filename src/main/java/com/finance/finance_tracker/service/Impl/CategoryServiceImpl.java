package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.mapper.CategoryMapper;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        User user = dto.getId() != null
                ? userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                : null;

        if (categoryRepository.existsByNameAndUserId(dto.getName(), user.getId())) {
            throw new IllegalArgumentException("Category already exists for this user");
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setBudget(dto.getBudget());
        category.setTransactions(dto.getTransactions());

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    public CategoryDto getCategoryById(Long id) {
        return categoryMapper.toDto(categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found")));
    }

    public List<CategoryDto> getAllCategories() {
        List<Category> list = categoryRepository.findAll();
        return list.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> getUserCategories(Long userId) {
        List<Category> list =  categoryRepository.findByUserId(userId);
        return list.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));

        if (!category.getTransactions().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated transactions");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public List<CategoryDto> findWithoutBudget(Long userId) {
        List<Category> categories = categoryRepository.findByUserIdAndBudgetIsNull(userId);

        return categories.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
