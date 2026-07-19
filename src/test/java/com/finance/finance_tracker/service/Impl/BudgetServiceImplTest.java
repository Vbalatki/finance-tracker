package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.mapper.BudgetMapper;
import com.finance.finance_tracker.repository.BudgetRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.TransactionRepository;
import com.finance.finance_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link BudgetServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        category = new Category();
        category.setId(5L);
        category.setName("Продукты");
        category.setUser(user);
    }

    @Nested
    @DisplayName("getBudgetsByUserId")
    class GetBudgetsByUserId {

        @Test
        @DisplayName("подставляет текущие траты за месяц в каждый бюджет")
        void getBudgetsByUserId_setsCurrentSpending() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setMonthlyLimit(new BigDecimal("5000.00"));
            budget.setCategory(category);
            budget.setUser(user);

            BudgetDto dto = new BudgetDto();
            dto.setCategoryId(5L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(budgetRepository.findByUserWithCategory(user)).thenReturn(List.of(budget));
            when(budgetMapper.toDto(budget)).thenReturn(dto);
            when(transactionRepository.getCurrentMonthExpenseByCategory(5L))
                    .thenReturn(new BigDecimal("1200.00"));

            List<BudgetDto> result = budgetService.getBudgetsByUserId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCurrentSpending()).isEqualByComparingTo("1200.00");
        }

        @Test
        @DisplayName("если трат за месяц нет (null) — подставляет ноль")
        void getBudgetsByUserId_nullSpending_defaultsToZero() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setCategory(category);
            budget.setUser(user);

            BudgetDto dto = new BudgetDto();
            dto.setCategoryId(5L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(budgetRepository.findByUserWithCategory(user)).thenReturn(List.of(budget));
            when(budgetMapper.toDto(budget)).thenReturn(dto);
            when(transactionRepository.getCurrentMonthExpenseByCategory(5L)).thenReturn(null);

            List<BudgetDto> result = budgetService.getBudgetsByUserId(1L);

            assertThat(result.get(0).getCurrentSpending()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не найден")
        void getBudgetsByUserId_userNotFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> budgetService.getBudgetsByUserId(99L));
        }

        @Test
        @DisplayName("возвращает пустой список, если у пользователя нет бюджетов")
        void getBudgetsByUserId_empty_returnsEmptyList() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(budgetRepository.findByUserWithCategory(user)).thenReturn(List.of());

            List<BudgetDto> result = budgetService.getBudgetsByUserId(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveBudget")
    class SaveBudget {

        @Test
        @DisplayName("создаёт новый бюджет, если для категории его ещё нет")
        void saveBudget_createsNew_whenNoneExists() {
            BudgetDto dto = new BudgetDto();
            dto.setCategoryId(5L);
            dto.setMonthlyLimit(new BigDecimal("3000.00"));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(budgetRepository.findByUserAndCategory(user, category)).thenReturn(Optional.empty());
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
            when(budgetMapper.toDto(any(Budget.class))).thenReturn(dto);

            budgetService.saveBudget(dto, 1L);

            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetRepository).save(captor.capture());
            Budget saved = captor.getValue();
            assertThat(saved.getMonthlyLimit()).isEqualByComparingTo("3000.00");
            assertThat(saved.getCurrentSpending()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getCategory()).isEqualTo(category);
        }

        @Test
        @DisplayName("обновляет месячный лимит существующего бюджета")
        void saveBudget_updatesExisting_whenAlreadyPresent() {
            Budget existing = new Budget();
            existing.setId(7L);
            existing.setMonthlyLimit(new BigDecimal("1000.00"));
            existing.setCurrentSpending(new BigDecimal("400.00"));
            existing.setUser(user);
            existing.setCategory(category);

            BudgetDto dto = new BudgetDto();
            dto.setCategoryId(5L);
            dto.setMonthlyLimit(new BigDecimal("2000.00"));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(budgetRepository.findByUserAndCategory(user, category)).thenReturn(Optional.of(existing));
            when(budgetRepository.save(existing)).thenReturn(existing);
            when(budgetMapper.toDto(existing)).thenReturn(dto);

            budgetService.saveBudget(dto, 1L);

            assertThat(existing.getMonthlyLimit()).isEqualByComparingTo("2000.00");
            // updates не должны сбрасывать текущие траты
            assertThat(existing.getCurrentSpending()).isEqualByComparingTo("400.00");
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не найден")
        void saveBudget_userNotFound_throws() {
            BudgetDto dto = new BudgetDto();
            dto.setCategoryId(5L);
            dto.setMonthlyLimit(BigDecimal.TEN);

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> budgetService.saveBudget(dto, 1L));
            verify(budgetRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если категория не найдена")
        void saveBudget_categoryNotFound_throws() {
            BudgetDto dto = new BudgetDto();
            dto.setCategoryId(99L);
            dto.setMonthlyLimit(BigDecimal.TEN);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> budgetService.saveBudget(dto, 1L));
        }
    }

    @Nested
    @DisplayName("resetSpending")
    class ResetSpending {

        @Test
        @DisplayName("обнуляет текущие траты бюджета")
        void resetSpending_setsCurrentSpendingToZero() {
            Budget budget = new Budget();
            budget.setId(1L);
            budget.setCurrentSpending(new BigDecimal("500.00"));

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.save(budget)).thenReturn(budget);

            budgetService.resetSpending(1L);

            assertThat(budget.getCurrentSpending()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если бюджет не найден")
        void resetSpending_notFound_throws() {
            when(budgetRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> budgetService.resetSpending(404L));
        }
    }

    @Nested
    @DisplayName("deleteBudget")
    class DeleteBudget {

        @Test
        @DisplayName("удаляет существующий бюджет")
        void deleteBudget_success() {
            when(budgetRepository.existsById(1L)).thenReturn(true);

            budgetService.deleteBudget(1L);

            verify(budgetRepository).deleteById(1L);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если бюджет не найден")
        void deleteBudget_notFound_throws() {
            when(budgetRepository.existsById(404L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> budgetService.deleteBudget(404L));
            verify(budgetRepository, never()).deleteById(any());
        }
    }
}
