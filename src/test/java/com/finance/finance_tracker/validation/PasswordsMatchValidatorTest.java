package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.dto.ChangePasswordDto;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PasswordsMatchValidatorTest {

    private final PasswordsMatchValidator validator = new PasswordsMatchValidator();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Test
    @DisplayName("совпадающие пароли — валидно")
    void isValid_matchingPasswords_returnsTrue() {
        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setNewPassword("newPassword123");
        dto.setConfirmPassword("newPassword123");

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("несовпадающие пароли — невалидно")
    void isValid_mismatchedPasswords_returnsFalse() {
        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setNewPassword("newPassword123");
        dto.setConfirmPassword("different");

        assertThat(validator.isValid(dto, context)).isFalse();
    }

    @Test
    @DisplayName("newPassword == null — валидно, это забота @NotBlank на самом поле")
    void isValid_nullNewPassword_returnsTrue() {
        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setNewPassword(null);
        dto.setConfirmPassword("something");

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("confirmPassword == null — валидно, это забота @NotBlank на самом поле")
    void isValid_nullConfirmPassword_returnsTrue() {
        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setNewPassword("something");
        dto.setConfirmPassword(null);

        assertThat(validator.isValid(dto, context)).isTrue();
    }
}
