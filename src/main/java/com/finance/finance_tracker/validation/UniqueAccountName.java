package com.finance.finance_tracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueAccountNameValidator.class)
public @interface UniqueAccountName {
    String message() default "Счёт с таким именем уже существует";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
