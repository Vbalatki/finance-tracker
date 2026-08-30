package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.dto.ChangePasswordDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

// Без @Component намеренно: нет зависимостей для внедрения, обычный new()
// через рефлексию тут не проблема (в отличие от Unique*Validator).
public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, ChangePasswordDto> {

    @Override
    public boolean isValid(ChangePasswordDto dto, ConstraintValidatorContext context) {
        if (dto.getNewPassword() == null || dto.getConfirmPassword() == null) {
            return true;
        }
        boolean matches = Objects.equals(dto.getNewPassword(), dto.getConfirmPassword());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return matches;
    }
}
