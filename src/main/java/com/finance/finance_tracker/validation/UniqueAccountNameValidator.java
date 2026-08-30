package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.util.SecurityUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueAccountNameValidator implements ConstraintValidator<UniqueAccountName, AccountDto> {

    private final AccountRepository accountRepository;

    @Override
    public boolean isValid(AccountDto dto, ConstraintValidatorContext context) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            return true;
        }

        // Намеренно не dto.getUserId() — это поле с формы, а не источник
        // истины о том, чей это счёт (см. AccountController.createAccount,
        // где dto.setUserId(...) вызывается уже ПОСЛЕ @Valid).
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
