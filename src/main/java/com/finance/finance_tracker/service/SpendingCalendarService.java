package com.finance.finance_tracker.service;

import com.finance.finance_tracker.dto.CalendarMonthDto;

import java.time.YearMonth;

public interface SpendingCalendarService {
    CalendarMonthDto getMonth(Long userId, YearMonth month);
}