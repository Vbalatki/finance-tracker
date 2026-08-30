package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.finance.finance_tracker.util.DataConstants.INCORRECT_CURRENT_PASSWORD;

@Component
@RequiredArgsConstructor
public class PasswordValidator {

    private final PasswordEncoder passwordEncoder;

    public void validateCurrentPassword(User user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidDataException(INCORRECT_CURRENT_PASSWORD);
        }
    }
}
