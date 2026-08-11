package com.finance.finance_tracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("equals — только по id, email не участвует")
    void equals_sameId_equalRegardlessOfOtherFields() {
        User a = new User(); a.setId(1L); a.setEmail("a@example.com");
        User b = new User(); b.setId(1L); b.setEmail("b@example.com");
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("equals — разные id не равны")
    void equals_differentId_notEqual() {
        User a = new User(); a.setId(1L);
        User b = new User(); b.setId(2L);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals — с null id не равны никому")
    void equals_nullId_notEqualToAnything() {
        assertThat(new User()).isNotEqualTo(new User());
    }

    @Test
    @DisplayName("hashCode — константный, не зависит от id")
    void hashCode_isConstantRegardlessOfId() {
        User withId = new User(); withId.setId(1L);
        assertThat(withId.hashCode()).isEqualTo(new User().hashCode());
    }
}
