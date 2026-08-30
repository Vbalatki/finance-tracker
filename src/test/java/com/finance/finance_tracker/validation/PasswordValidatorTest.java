package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordValidator passwordValidator;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setPassword("encodedPassword");
    }

    @Test
    @DisplayName("не бросает исключение, если текущий пароль верен")
    void validateCurrentPassword_correctPassword_doesNotThrow() {
        when(passwordEncoder.matches("correctPass", "encodedPassword")).thenReturn(true);

        assertDoesNotThrow(() -> passwordValidator.validateCurrentPassword(user, "correctPass"));
    }

    @Test
    @DisplayName("бросает InvalidDataException, если текущий пароль неверен")
    void validateCurrentPassword_wrongPassword_throws() {
        when(passwordEncoder.matches("wrongPass", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidDataException.class,
                () -> passwordValidator.validateCurrentPassword(user, "wrongPass"));
    }
}
