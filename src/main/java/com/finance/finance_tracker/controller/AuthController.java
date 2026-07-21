package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf-контроллер публичных страниц входа и регистрации. Доступ не
 * требует аутентификации (см. {@code permitAll()} для {@code /login} и
 * {@code /register} в {@code SecurityConfig}).
 */
@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    /**
     * Страница логина.
     *
     * @return {@code "auth/login"}
     */
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /**
     * Страница формы регистрации.
     *
     * @param model модель представления
     * @return {@code "auth/register"}
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "auth/register";
    }

    /**
     * Обрабатывает отправку формы регистрации. Если email уже
     * зарегистрирован, ошибка отображается как ошибка поля {@code email}.
     *
     * @param dto                данные регистрации
     * @param result             результат валидации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /login} при успехе, иначе {@code "auth/register"}
     */
    @PostMapping("/register")
    public String register(@ModelAttribute("userDto") @Valid UserDto dto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(dto);
            redirectAttributes.addFlashAttribute("success", "Регистрация успешна! Войдите в систему.");
            return "redirect:/login";
        } catch (Exception e) {
            result.rejectValue("email", "error.user", e.getMessage());
            return "auth/register";
        }
    }
}
