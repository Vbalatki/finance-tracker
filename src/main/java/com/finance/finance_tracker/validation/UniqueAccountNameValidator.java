package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.util.SecurityUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueAccountNameValidator implements ConstraintValidator<UniqueAccountName, UniqueAccountNameOwner> {

    private final AccountRepository accountRepository;

    @Override
    public boolean isValid(UniqueAccountNameOwner dto, ConstraintValidatorContext context) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            return true;
        }

        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return true;
        }

        boolean duplicate = dto.getId() != null
                ? accountRepository.existsByNameAndUserIdAndIdNot(dto.getName(), currentUserId, dto.getId())
                : accountRepository.existsByNameAndUserId(dto.getName(), currentUserId);

        if (duplicate) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("name")
                    .addConstraintViolation();
        }
        return !duplicate;
    }
}