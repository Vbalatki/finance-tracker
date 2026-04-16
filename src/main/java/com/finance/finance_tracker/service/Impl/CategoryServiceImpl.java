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
    private final CategoryMapper categoryMapper;
    private final UserRepository userRepository;

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        if (categoryRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Категория с таким именем уже существует");
        }
        Category category = new Category();
        category.setName(dto.getName());
        category.setUser(user);
        System.out.println("Saving category with name: " + category.getName()
                + ", user: " + (category.getUser() != null ?
                category.getUser().getId() : "null"));

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    public CategoryDto getCategoryById(Long id) {
        return categoryMapper.toDto(categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена")));
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllOrderById().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateCategory(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Категория с таким именем уже существует");
        }
        category.setName(name);
        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена по id: " + id));
        if (!category.getTransactions().isEmpty()) {
            throw new IllegalStateException("Невозможно удалить категорию, так как есть связанные транзакции");
        }
        categoryRepository.delete(category);
    }
}