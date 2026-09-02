package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.CalendarMonthDto;
import com.finance.finance_tracker.service.SpendingCalendarService;
import com.finance.finance_tracker.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DateTimeException;
import java.time.YearMonth;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final SpendingCalendarService spendingCalendarService;

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month,
                           Model model) {
        Long userId = SecurityUtil.getCurrentUserId();

        YearMonth targetMonth;
        if (year != null && month != null) {
            try {
                targetMonth = YearMonth.of(year, month);
            } catch (DateTimeException e) {
                log.warn("Некорректные year/month в запросе календаря: year={}, month={} — редирект на /calendar",
                        year, month);
                return "redirect:/calendar";
            }
        } else {
            targetMonth = YearMonth.now();
        }

        CalendarMonthDto calendarMonth = spendingCalendarService.getMonth(userId, targetMonth);

        model.addAttribute("calendarMonth", calendarMonth);
        model.addAttribute("prevMonth", targetMonth.minusMonths(1));
        model.addAttribute("nextMonth", targetMonth.plusMonths(1));

        return "calendar/index";
    }
}