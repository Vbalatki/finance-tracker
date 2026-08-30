package com.finance.finance_tracker.testsupport;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Фабрика валидаторов для standalone MockMvc-тестов. Обычные constraint'ы
 * (@NotBlank и т.п.) имеют no-arg конструктор и создаются как есть.
 * Кастомные валидаторы с DI (UniqueEmailValidator и подобные, которым
 * нужен репозиторий) не имеют no-arg конструктора — вместо падения с
 * NoSuchMethodException подставляется Mockito-заглушка, всегда
 * возвращающая true. Реальная логика этих валидаторов проверяется
 * отдельными юнит-тестами на сам валидатор (test/validation/*).
 */
public class PermissiveConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        try {
            return key.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            T stub = mock(key);
            when(stub.isValid(any(), any())).thenReturn(true);
            return stub;
        }
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
        // без состояния, освобождать нечего
    }
}
