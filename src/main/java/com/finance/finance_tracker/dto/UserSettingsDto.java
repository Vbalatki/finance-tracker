package com.finance.finance_tracker.dto;

import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.Theme;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserSettingsDto {

    @NotNull(message = "Тема оформления обязательна")
    private Theme theme;

    @NotNull(message = "Основная валюта обязательна")
    private Currency defaultCurrency;
}