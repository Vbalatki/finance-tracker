package com.finance.finance_tracker.controller.advice;

import com.finance.finance_tracker.util.SecurityUtil;
import com.finance.finance_tracker.entity.enums.Theme;
import com.finance.finance_tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Добавляет тему оформления текущего пользователя в модель КАЖДОГО
 * запроса — без этого пришлось бы прописывать её в каждом контроллере
 * по отдельности. Применяется в layout/head.html через meta-тег +
 * маленький inline-скрипт, тот же приём, что уже используется для CSRF.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class ThemeControllerAdvice {

    private final UserService userService;

    @ModelAttribute("currentTheme")
    public String currentTheme() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Theme.LIGHT.name();
        }
        try {
            return userService.getUserSettings(userId).getTheme().name();
        } catch (Exception e) {
            return Theme.LIGHT.name();
        }
    }
}