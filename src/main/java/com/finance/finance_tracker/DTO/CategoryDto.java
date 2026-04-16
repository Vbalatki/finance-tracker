package com.finance.finance_tracker.DTO;


import com.finance.finance_tracker.entity.User;
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
}