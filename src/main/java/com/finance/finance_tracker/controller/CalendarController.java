package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.CalendarMonthDto;
import com.finance.finance_tracker.util.SecurityUtil;
import com.finance.finance_tracker.service.SpendingCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final SpendingCalendarService spendingCalendarService;

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month,
                           Model model) {
        Long userId = SecurityUtil.getCurrentUserId();

        YearMonth targetMonth = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        CalendarMonthDto calendarMonth = spendingCalendarService.getMonth(userId, targetMonth);

        model.addAttribute("calendarMonth", calendarMonth);
        model.addAttribute("prevMonth", targetMonth.minusMonths(1));
        model.addAttribute("nextMonth", targetMonth.plusMonths(1));

        return "calendar/index";
    }
}