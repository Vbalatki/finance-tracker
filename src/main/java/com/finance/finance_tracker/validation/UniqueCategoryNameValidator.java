package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.util.SecurityUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueCategoryNameValidator implements ConstraintValidator<UniqueCategoryName, CategoryDto> {

    private final CategoryRepository categoryRepository;

    @Override
    public boolean isValid(CategoryDto dto, ConstraintValidatorContext context) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            return true;
        }
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return true;
        }

        boolean duplicate = dto.getId() != null
                ? categoryRepository.existsByNameVisibleToUserAndIdNot(dto.getName(), currentUserId, dto.getId())
                : categoryRepository.existsByNameVisibleToUser(dto.getName(), currentUserId);

        if (duplicate) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("name")
                    .addConstraintViolation();
        }
        return !duplicate;
    }
}
