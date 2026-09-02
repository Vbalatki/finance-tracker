package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, UniqueEmailOwner> {

    private final UserRepository userRepository;

    @Override
    public boolean isValid(UniqueEmailOwner dto, ConstraintValidatorContext context) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return true;
        }

        boolean unique = userRepository.findByEmail(dto.getEmail())
                .map(existing -> existing.getId().equals(dto.getId()))
                .orElse(true);

        if (!unique) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("email")
                    .addConstraintViolation();
        }
        return unique;
    }
}