package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.BudgetDto;
import com.finance.finance_tracker.entity.Budget;
import com.finance.finance_tracker.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetMapperTest {

    private final BudgetMapper mapper = new BudgetMapperImpl();

    @Test
    @DisplayName("toDto подставляет category.id/name из вложенной категории")
    void toDto_mapsNestedCategory() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Продукты");

        Budget budget = new Budget();
        budget.setId(1L);
        budget.setMonthlyLimit(new BigDecimal("5000.00"));
        budget.setCategory(category);

        BudgetDto dto = mapper.toDto(budget);
        assertThat(dto.getCategoryId()).isEqualTo(5L);
        assertThat(dto.getCategoryName()).isEqualTo("Продукты");
    }

    @Test
    @DisplayName("toEntity игнорирует id из dto")
    void toEntity_ignoresIdFromDto() {
        BudgetDto dto = new BudgetDto();
        dto.setId(999L);
        dto.setMonthlyLimit(BigDecimal.TEN);
        dto.setCategoryId(5L);

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }
}
