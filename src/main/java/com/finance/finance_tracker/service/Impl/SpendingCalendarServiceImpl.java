package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.dto.CalendarDayDto;
import com.finance.finance_tracker.dto.CalendarMonthDto;
import com.finance.finance_tracker.dto.PlannedChargeDto;
import com.finance.finance_tracker.entity.RecurringCommitment;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.repository.RecurringCommitmentRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.service.SpendingCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpendingCalendarServiceImpl implements SpendingCalendarService {

    private final TransactionRepository transactionRepository;
    private final RecurringCommitmentRepository recurringCommitmentRepository;

    @Override
    @Transactional(readOnly = true)
    public CalendarMonthDto getMonth(Long userId, YearMonth month) {
        log.debug("Построение календаря трат: userId={}, month={}", userId, month);

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<Transaction> transactions = transactionRepository.findByUserIdAndCreatedAtBetween(
                userId, monthStart.atStartOfDay(), monthEnd.plusDays(1).atStartOfDay());

        Map<LocalDate, BigDecimal> expenseByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> incomeByDay = new HashMap<>();
        for (Transaction t : transactions) {
            LocalDate day = t.getCreatedAt().toLocalDate();
            Map<LocalDate, BigDecimal> target = t.getType() == TransactionType.EXPENSE ? expenseByDay : incomeByDay;
            target.merge(day, t.getAmount(), BigDecimal::add);
        }

        BigDecimal maxDayExpense = expenseByDay.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<RecurringCommitment> commitments = recurringCommitmentRepository.findActiveByUserId(userId);
        Map<Integer, List<RecurringCommitment>> commitmentsByDay = commitments.stream()
                .collect(Collectors.groupingBy(c -> Math.min(c.getDayOfMonth(), month.lengthOfMonth())));

        LocalDate gridStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<CalendarDayDto> days = new ArrayList<>();
        for (LocalDate date = gridStart; !date.isAfter(gridEnd); date = date.plusDays(1)) {
            boolean isCurrentMonth = YearMonth.from(date).equals(month);
            boolean isFuture = date.isAfter(today);

            BigDecimal expense = expenseByDay.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal income = incomeByDay.getOrDefault(date, BigDecimal.ZERO);

            double intensity = maxDayExpense.compareTo(BigDecimal.ZERO) > 0
                    ? expense.divide(maxDayExpense, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            List<PlannedChargeDto> planned = isCurrentMonth && isFuture
                    ? commitmentsByDay.getOrDefault(date.getDayOfMonth(), List.of()).stream()
                    .map(c -> new PlannedChargeDto(c.getName(), c.getAmount(), c.getType()))
                    .collect(Collectors.toList())
                    : List.of();

            days.add(new CalendarDayDto(date, isCurrentMonth, date.equals(today), isFuture,
                    expense, income, intensity, planned));
        }

        BigDecimal totalExpense = expenseByDay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = incomeByDay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CalendarMonthDto(month, days, totalExpense, totalIncome);
    }
}