package com.finance.finance_tracker.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryDto {

    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private int transactionsCount;

    private Long userId;

    // true для стандартных категорий — видны всем, недоступны для редактирования/удаления.
    private boolean defaultCategory;
}