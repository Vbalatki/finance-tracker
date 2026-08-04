package com.finance.finance_tracker.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record CalendarMonthDto(
        YearMonth month,
        List<CalendarDayDto> days,
        BigDecimal totalExpense,
        BigDecimal totalIncome
) {
}