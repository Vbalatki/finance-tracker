package com.finance.finance_tracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringCommitmentTest {

    @Test
    @DisplayName("equals — только по id")
    void equals_sameId_equal() {
        RecurringCommitment a = new RecurringCommitment(); a.setId(1L);
        RecurringCommitment b = new RecurringCommitment(); b.setId(1L);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("hashCode — константный")
    void hashCode_isConstant() {
        RecurringCommitment withId = new RecurringCommitment(); withId.setId(1L);
        assertThat(withId.hashCode()).isEqualTo(new RecurringCommitment().hashCode());
    }
}
