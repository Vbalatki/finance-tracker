package com.finance.finance_tracker.DTO;

import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CategoryDto {
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private List<Transaction> transactions;

    @NotNull(message = "UserId cannot be null")
    private Long userId;

    private Budget budget;

    private User user;
}
