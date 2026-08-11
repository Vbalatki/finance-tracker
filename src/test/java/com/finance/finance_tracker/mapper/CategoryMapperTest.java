package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapperImpl();

    @Test
    @DisplayName("toDto помечает категорию без user как стандартную")
    void toDto_noUser_marksAsDefault() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Транспорт");

        assertThat(mapper.toDto(category).isDefaultCategory()).isTrue();
    }

    @Test
    @DisplayName("toDto с заполненным user не помечает категорию как стандартную")
    void toDto_withUser_notDefault() {
        User user = new User();
        user.setId(1L);

        Category category = new Category();
        category.setId(5L);
        category.setUser(user);

        assertThat(mapper.toDto(category).isDefaultCategory()).isFalse();
    }

    @Test
    @DisplayName("toEntity игнорирует id из dto даже если он заполнен (S5)")
    void toEntity_ignoresIdFromDto() {
        CategoryDto dto = new CategoryDto();
        dto.setId(999L);
        dto.setName("Категория");
        dto.setUserId(1L);

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }
}
