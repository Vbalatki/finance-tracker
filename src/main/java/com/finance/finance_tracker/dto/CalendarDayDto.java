package com.finance.finance_tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CalendarDayDto(
        LocalDate date,
        boolean currentMonth,
        boolean today,
        boolean future,
        BigDecimal totalExpense,
        BigDecimal totalIncome,
        double intensity,
        List<PlannedChargeDto> plannedCharges
) {
}