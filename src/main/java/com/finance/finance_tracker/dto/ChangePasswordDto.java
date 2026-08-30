package com.finance.finance_tracker.dto;

import com.finance.finance_tracker.util.DataConstants;
import com.finance.finance_tracker.validation.PasswordsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PasswordsMatch
public class ChangePasswordDto {

    @NotBlank(message = "Введите текущий пароль")
    private String currentPassword;

    @NotBlank(message = "Введите новый пароль")
    @Size(min = DataConstants.MIN_PASSWORD_LENGTH, max = 255,
            message = "Пароль должен быть не короче " + DataConstants.MIN_PASSWORD_LENGTH + " символов")
    private String newPassword;

    @NotBlank(message = "Подтвердите новый пароль")
    private String confirmPassword;
}
