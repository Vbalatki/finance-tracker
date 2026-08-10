package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.CalendarDayDto;
import com.finance.finance_tracker.dto.CalendarMonthDto;
import com.finance.finance_tracker.entity.RecurringCommitment;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.repository.RecurringCommitmentRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Unit-тесты для {@link SpendingCalendarServiceImpl}.
 *
 * <p>Сервис нигде не инжектирует {@link java.time.Clock} и использует
 * {@code LocalDate.now()} напрямую — из-за этого часть логики (today/future)
 * нельзя протестировать с фиксированной датой. Тесты на суммы/интенсивность
 * используют заведомо прошедший месяц (не зависят от today/future вообще),
 * а тесты на "запланированные платежи" используют {@code YearMonth.now()}
 * с {@link Assumptions#assumeTrue} на граничные дни месяца, чтобы не быть
 * хрупкими в последний день месяца. Если решите инжектировать Clock в сервис —
 * все assumeTrue тут же можно будет убрать и тестировать с фиксированной датой.
 */
@ExtendWith(MockitoExtension.class)
class SpendingCalendarServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private RecurringCommitmentRepository recurringCommitmentRepository;

    @InjectMocks
    private SpendingCalendarServiceImpl calendarService;

    private static final Long USER_ID = 1L;

    private Transaction transaction(LocalDate date, BigDecimal amount, TransactionType type) {
        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setType(type);
        t.setCreatedAt(date.atTime(12, 0));
        return t;
    }

    private RecurringCommitment commitment(String name, BigDecimal amount, TransactionType type, int dayOfMonth) {
        RecurringCommitment c = new RecurringCommitment();
        c.setName(name);
        c.setAmount(amount);
        c.setType(type);
        c.setDayOfMonth(dayOfMonth);
        c.setActive(true);
        return c;
    }

    private CalendarDayDto dayOf(CalendarMonthDto result, LocalDate date) {
        return result.days().stream()
                .filter(d -> d.date().equals(date))
                .findFirst()
                .orElseThrow(() -> new AssertionError("День " + date + " не найден в сетке календаря"));
    }

    @Nested
    @DisplayName("суммы и группировка по дням")
    class Totals {

        private final YearMonth pastMonth = YearMonth.of(2023, 6);

        @BeforeEach
        void stubRecurringCommitments() {
            when(recurringCommitmentRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());
        }

        @Test
        @DisplayName("транзакции группируются по дню создания, расходы и доходы — раздельно")
        void groupsTransactionsByDay() {
            LocalDate day15 = pastMonth.atDay(15);
            LocalDate day16 = pastMonth.atDay(16);
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            transaction(day15, new BigDecimal("500.00"), TransactionType.EXPENSE),
                            transaction(day15, new BigDecimal("300.00"), TransactionType.EXPENSE),
                            transaction(day15, new BigDecimal("1000.00"), TransactionType.INCOME),
                            transaction(day16, new BigDecimal("200.00"), TransactionType.EXPENSE)
                    ));

            CalendarMonthDto result = calendarService.getMonth(USER_ID, pastMonth);

            assertThat(dayOf(result, day15).totalExpense()).isEqualByComparingTo("800.00");
            assertThat(dayOf(result, day15).totalIncome()).isEqualByComparingTo("1000.00");
            assertThat(dayOf(result, day16).totalExpense()).isEqualByComparingTo("200.00");
            assertThat(dayOf(result, day16).totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("день без транзакций — нули, а не null")
        void dayWithoutTransactions_returnsZero() {
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of());

            CalendarMonthDto result = calendarService.getMonth(USER_ID, pastMonth);

            CalendarDayDto anyDay = dayOf(result, pastMonth.atDay(10));
            assertThat(anyDay.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(anyDay.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("totalExpense/totalIncome месяца — сумма по всем дням")
        void monthTotals_sumAcrossAllDays() {
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            transaction(pastMonth.atDay(1), new BigDecimal("100.00"), TransactionType.EXPENSE),
                            transaction(pastMonth.atDay(2), new BigDecimal("50.00"), TransactionType.EXPENSE),
                            transaction(pastMonth.atDay(3), new BigDecimal("2000.00"), TransactionType.INCOME)
                    ));

            CalendarMonthDto result = calendarService.getMonth(USER_ID, pastMonth);

            assertThat(result.totalExpense()).isEqualByComparingTo("150.00");
            assertThat(result.totalIncome()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("запрос в репозиторий уходит с границами ровно этого месяца")
        void queriesRepositoryWithCorrectMonthBoundaries() {
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of());

            calendarService.getMonth(USER_ID, pastMonth);

            verify(transactionRepository).findByUserIdAndCreatedAtBetween(
                    USER_ID,
                    pastMonth.atDay(1).atStartOfDay(),
                    pastMonth.atEndOfMonth().plusDays(1).atStartOfDay());
        }
    }

    @Nested
    @DisplayName("интенсивность (для heatmap)")
    class Intensity {

        private final YearMonth pastMonth = YearMonth.of(2023, 6);

        @BeforeEach
        void stubRecurringCommitments() {
            when(recurringCommitmentRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());
        }

        @Test
        @DisplayName("день с максимальным расходом за месяц получает интенсивность 1.0")
        void maxExpenseDay_getsIntensityOne() {
            LocalDate maxDay = pastMonth.atDay(10);
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            transaction(maxDay, new BigDecimal("1000.00"), TransactionType.EXPENSE),
                            transaction(pastMonth.atDay(5), new BigDecimal("500.00"), TransactionType.EXPENSE)
                    ));

            CalendarMonthDto result = calendarService.getMonth(USER_ID, pastMonth);

            assertThat(dayOf(result, maxDay).intensity()).isEqualTo(1.0);
            assertThat(dayOf(result, pastMonth.atDay(5)).intensity()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("нет расходов за весь месяц — интенсивность 0.0 везде, деления на ноль нет")
        void noExpensesAtAll_intensityZeroEverywhere() {
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of(
                            transaction(pastMonth.atDay(5), new BigDecimal("1000.00"), TransactionType.INCOME)
                    ));

            CalendarMonthDto result = calendarService.getMonth(USER_ID, pastMonth);

            assertThat(result.days()).extracting(CalendarDayDto::intensity).allMatch(i -> i == 0.0);
        }
    }

    @Nested
    @DisplayName("границы сетки")
    class GridBoundaries {

        private final YearMonth pastMonth = YearMonth.of(2023, 6);

        @BeforeEach
        void stubs() {
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of());
            when(recurringCommitmentRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());
        }

        @Test
        @DisplayName("сетка всегда начинается с понедельника и заканчивается воскресеньем")
        void gridStartsMonday_endsSunday() {
            CalendarMonthDto result = calendarService.getMonth(USER_ID, pastMonth);

            assertThat(result.days().get(0).date().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(result.days().get(result.days().size() - 1).date().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        }

        @Test
        @DisplayName("дни соседних месяцев в сетке помечены currentMonth=false")
        void adjacentMonthDays_markedAsNotCurrentMonth() {
            CalendarMonthDto result = calendarService.getMonth(USER_ID, pastMonth);

            boolean hasLeadingOrTrailingDay = result.days().stream().anyMatch(d -> !d.currentMonth());
            assertThat(hasLeadingOrTrailingDay).isTrue();
            assertThat(dayOf(result, pastMonth.atDay(15)).currentMonth()).isTrue();
        }
    }

    @Nested
    @DisplayName("запланированные платежи (recurring commitments)")
    class PlannedCharges {

        private final YearMonth currentMonth = YearMonth.now();
        private final LocalDate today = LocalDate.now();

        @BeforeEach
        void stubTransactions() {
            when(transactionRepository.findByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("платёж на будущий день текущего месяца попадает в plannedCharges этого дня")
        void futureCommitment_appearsOnCorrectDay() {
            Assumptions.assumeTrue(today.getDayOfMonth() < currentMonth.lengthOfMonth(),
                    "Пропущено: сегодня последний день месяца, будущего дня в этом месяце не существует");

            LocalDate futureDay = today.plusDays(1);
            when(recurringCommitmentRepository.findActiveByUserId(USER_ID))
                    .thenReturn(List.of(commitment("Netflix", new BigDecimal("999.00"),
                            TransactionType.EXPENSE, futureDay.getDayOfMonth())));

            CalendarMonthDto result = calendarService.getMonth(USER_ID, currentMonth);

            assertThat(dayOf(result, futureDay).plannedCharges())
                    .extracting("name", "amount", "type")
                    .containsExactly(org.assertj.core.groups.Tuple.tuple(
                            "Netflix", new BigDecimal("999.00"), TransactionType.EXPENSE));
        }

        @Test
        @DisplayName("платёж на уже прошедший день текущего месяца не показывается")
        void pastDayCommitment_doesNotAppear() {
            Assumptions.assumeTrue(today.getDayOfMonth() > 1,
                    "Пропущено: сегодня первый день месяца, прошедшего дня в этом месяце не существует");

            LocalDate pastDay = today.minusDays(1);
            when(recurringCommitmentRepository.findActiveByUserId(USER_ID))
                    .thenReturn(List.of(commitment("Уже прошло", BigDecimal.TEN,
                            TransactionType.EXPENSE, pastDay.getDayOfMonth())));

            CalendarMonthDto result = calendarService.getMonth(USER_ID, currentMonth);

            assertThat(dayOf(result, pastDay).plannedCharges()).isEmpty();
        }

        @Test
        @DisplayName("dayOfMonth больше длины месяца — прижимается к последнему дню (Math.min)")
        void dayOfMonthBeyondMonthLength_clampsToLastDay() {
            LocalDate lastDay = currentMonth.atEndOfMonth();
            Assumptions.assumeTrue(lastDay.isAfter(today),
                    "Пропущено: последний день месяца уже не в будущем");

            when(recurringCommitmentRepository.findActiveByUserId(USER_ID))
                    .thenReturn(List.of(commitment("Аренда", new BigDecimal("30000.00"),
                            TransactionType.EXPENSE, currentMonth.lengthOfMonth() + 5)));

            CalendarMonthDto result = calendarService.getMonth(USER_ID, currentMonth);

            assertThat(dayOf(result, lastDay).plannedCharges()).hasSize(1);
        }

        @Test
        @DisplayName("дни соседних месяцев в сетке никогда не показывают плановые платежи")
        void adjacentMonthDays_neverShowPlannedCharges() {
            List<RecurringCommitment> everyDay = new java.util.ArrayList<>();
            for (int day = 1; day <= 31; day++) {
                everyDay.add(commitment("Платёж " + day, BigDecimal.ONE, TransactionType.EXPENSE, day));
            }
            when(recurringCommitmentRepository.findActiveByUserId(USER_ID)).thenReturn(everyDay);

            CalendarMonthDto result = calendarService.getMonth(USER_ID, currentMonth);

            assertThat(result.days().stream().filter(d -> !d.currentMonth()))
                    .allMatch(d -> d.plannedCharges().isEmpty());
        }
    }
}