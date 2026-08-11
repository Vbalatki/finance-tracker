package com.finance.finance_tracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    @DisplayName("equals — только по id")
    void equals_sameId_equal() {
        Category a = new Category(); a.setId(1L);
        Category b = new Category(); b.setId(1L);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("isDefaultCategory — true, если user не задан")
    void isDefaultCategory_noUser_true() {
        assertThat(new Category().isDefaultCategory()).isTrue();
    }

    @Test
    @DisplayName("isDefaultCategory — false для категории с владельцем")
    void isDefaultCategory_withUser_false() {
        Category category = new Category();
        category.setUser(new User());
        assertThat(category.isDefaultCategory()).isFalse();
    }
}
