package com.finance.finance_tracker.dto;

import com.finance.finance_tracker.validation.UniqueCategoryName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@UniqueCategoryName
public class CategoryDto {

    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private int transactionsCount;

    private Long userId;

    private boolean defaultCategory;
}
