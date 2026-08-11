package com.finance.finance_tracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    @DisplayName("equals — только по id")
    void equals_sameId_equal() {
        Transaction a = new Transaction(); a.setId(1L);
        Transaction b = new Transaction(); b.setId(1L);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("equals — разные id не равны")
    void equals_differentId_notEqual() {
        Transaction a = new Transaction(); a.setId(1L);
        Transaction b = new Transaction(); b.setId(2L);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("hashCode — константный")
    void hashCode_isConstant() {
        Transaction withId = new Transaction(); withId.setId(1L);
        assertThat(withId.hashCode()).isEqualTo(new Transaction().hashCode());
    }
}
