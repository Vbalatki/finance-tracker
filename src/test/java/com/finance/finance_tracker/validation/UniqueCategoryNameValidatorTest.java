package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.CategoryRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueCategoryNameValidatorTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    private UniqueCategoryNameValidator validator;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("пустое имя валидно — это забота @NotBlank")
    void isValid_blankName_returnsTrue() {
        validator = new UniqueCategoryNameValidator(categoryRepository);
        CategoryDto dto = new CategoryDto();
        dto.setName("  ");

        assertThat(validator.isValid(dto, context)).isTrue();
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("создание новой категории со свободным именем — валидно")
    void isValid_creatingWithFreeName_returnsTrue() {
        authenticateAs(1L);
        when(categoryRepository.existsByNameVisibleToUser("Хобби", 1L)).thenReturn(false);

        validator = new UniqueCategoryNameValidator(categoryRepository);
        CategoryDto dto = new CategoryDto();
        dto.setName("Хобби");

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("создание категории с именем, занятым своей или стандартной — невалидно")
    void isValid_creatingWithDuplicateName_returnsFalse() {
        authenticateAs(1L);
        when(categoryRepository.existsByNameVisibleToUser("Продукты", 1L)).thenReturn(true);

        validator = new UniqueCategoryNameValidator(categoryRepository);
        CategoryDto dto = new CategoryDto();
        dto.setName("Продукты");

        assertThat(validator.isValid(dto, context)).isFalse();
    }

    @Test
    @DisplayName("при редактировании исключает саму категорию из проверки")
    void isValid_editingOwnCategory_excludesSelf_returnsTrue() {
        authenticateAs(1L);
        when(categoryRepository.existsByNameVisibleToUserAndIdNot("Продукты", 1L, 5L)).thenReturn(false);

        validator = new UniqueCategoryNameValidator(categoryRepository);
        CategoryDto dto = new CategoryDto();
        dto.setId(5L);
        dto.setName("Продукты");

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("при редактировании на имя, занятое другой категорией — невалидно")
    void isValid_editingToNameTakenByAnother_returnsFalse() {
        authenticateAs(1L);
        when(categoryRepository.existsByNameVisibleToUserAndIdNot("Занято", 1L, 5L)).thenReturn(true);

        validator = new UniqueCategoryNameValidator(categoryRepository);
        CategoryDto dto = new CategoryDto();
        dto.setId(5L);
        dto.setName("Занято");

        assertThat(validator.isValid(dto, context)).isFalse();
    }
}
