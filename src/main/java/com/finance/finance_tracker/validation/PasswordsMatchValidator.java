package com.finance.finance_tracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, PasswordConfirmation> {

    @Override
    public boolean isValid(PasswordConfirmation dto, ConstraintValidatorContext context) {
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