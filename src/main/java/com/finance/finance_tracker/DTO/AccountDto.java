package com.finance.finance_tracker.DTO;

import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccountDto {
    private Long id;

    @NotBlank(message = "AccountName cannot be null")
    @Size(min = 2, max = 255)
    private String name;

    @NotNull(message = "Balance cannot be null")
    private BigDecimal balance;

    @NotNull(message = "Currency cannot be null")
    private Currency currency;

    @NotNull(message = "UserId cannot be null")
    private Long userId;

    private List<Transaction> transactions = new ArrayList<>();
}