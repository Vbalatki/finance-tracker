package com.finance.finance_tracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    @DisplayName("equals — только по id")
    void equals_sameId_equal() {
        Account a = new Account(); a.setId(1L);
        Account b = new Account(); b.setId(1L);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("equals — разные id не равны")
    void equals_differentId_notEqual() {
        Account a = new Account(); a.setId(1L);
        Account b = new Account(); b.setId(2L);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("hashCode — константный")
    void hashCode_isConstant() {
        Account withId = new Account(); withId.setId(1L);
        assertThat(withId.hashCode()).isEqualTo(new Account().hashCode());
    }
}
