package com.finance.finance_tracker.dto;

import com.finance.finance_tracker.entity.enums.TransactionType;

import java.math.BigDecimal;

public record PlannedChargeDto(String name, BigDecimal amount, TransactionType type) {
}