package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.UserRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueEmailValidatorTest {

    @Mock
    private UserRepository userRepository;

    // RETURNS_DEEP_STUBS — чтобы context.buildConstraintViolationWithTemplate(...)
    // .addPropertyNode(...).addConstraintViolation() не падал с NPE в
    // негативном сценарии: обычный мок вернул бы null на первом же звене цепочки.
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    private UniqueEmailValidator validator;

    @Test
    @DisplayName("пустой email валиден — это забота @NotBlank/@Email")
    void isValid_blankEmail_returnsTrue() {
        validator = new UniqueEmailValidator(userRepository);
        UserDto dto = new UserDto();
        dto.setEmail("  ");

        assertThat(validator.isValid(dto, context)).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("свободный email — валиден")
    void isValid_emailNotTaken_returnsTrue() {
        when(userRepository.findByEmail("free@example.com")).thenReturn(Optional.empty());

        validator = new UniqueEmailValidator(userRepository);
        UserDto dto = new UserDto();
        dto.setEmail("free@example.com");

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("email занят другим пользователем — невалиден")
    void isValid_emailTakenByAnotherUser_returnsFalse() {
        User existing = new User();
        existing.setId(2L);
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

        validator = new UniqueEmailValidator(userRepository);
        UserDto dto = new UserDto();
        dto.setId(1L); // редактирует другой пользователь, не владелец email
        dto.setEmail("taken@example.com");

        assertThat(validator.isValid(dto, context)).isFalse();
    }

    @Test
    @DisplayName("email принадлежит самому себе при редактировании — валиден")
    void isValid_editingOwnEmail_returnsTrue() {
        User existing = new User();
        existing.setId(1L);
        when(userRepository.findByEmail("mine@example.com")).thenReturn(Optional.of(existing));

        validator = new UniqueEmailValidator(userRepository);
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setEmail("mine@example.com");

        assertThat(validator.isValid(dto, context)).isTrue();
    }
}
