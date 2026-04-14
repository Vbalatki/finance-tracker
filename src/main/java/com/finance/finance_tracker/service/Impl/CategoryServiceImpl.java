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

import java.util.ArrayList;
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
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        if (categoryRepository.existsByNameAndUserId(dto.getName(), user.getId())) {
            throw new IllegalArgumentException("Уже есть такая категория");
        }
        Category category = new Category();
        category.setName(dto.getName());
        category.setUser(user);

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    public CategoryDto getCategoryById(Long id) {
        return categoryMapper.toDto(categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена")));
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
                .map(category ->
                {
                    CategoryDto dto = categoryMapper.toDto(category);
                    dto.setTransactionsCount(category.getTransactions() != null?
                                            category.getTransactions().size(): 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateCategory(Long id, String name) {
        Category category  = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));
        category.setName(name);
        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена по id: " + id));

        if (!category.getTransactions().isEmpty()) {
            throw new IllegalStateException("Невозможно удалить категорию");
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
