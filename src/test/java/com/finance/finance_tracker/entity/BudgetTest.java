package com.finance.finance_tracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetTest {

    @Test
    @DisplayName("getRemainingAmount — лимит минус потрачено")
    void getRemainingAmount_subtractsSpendingFromLimit() {
        Budget budget = new Budget();
        budget.setMonthlyLimit(new BigDecimal("5000.00"));
        budget.setCurrentSpending(new BigDecimal("1200.00"));

        assertThat(budget.getRemainingAmount()).isEqualByComparingTo("3800.00");
    }

    @Test
    @DisplayName("equals — в отличие от остальных сущностей, Budget НЕ id-based (Lombok @Data по всем полям)")
    void equals_isFieldBased_unlikeOtherEntities() {
        Budget a = new Budget(); a.setId(1L); a.setMonthlyLimit(new BigDecimal("5000.00"));
        Budget b = new Budget(); b.setId(1L); b.setMonthlyLimit(new BigDecimal("3000.00"));

        // документирует текущее (непоследовательное) поведение, а не одобряет его
        assertThat(a).isNotEqualTo(b);
    }
}
