package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.service.UserService;
import com.finance.finance_tracker.service.impl.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final LoginAttemptService loginAttemptService;

    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            return "redirect:/dashboard";
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "auth/register";
    }

    /**
     * Обрабатывает отправку формы регистрации. Ограничение по IP — не
     * более 5 попыток за 15 минут, счётчик считает каждый POST независимо
     * от результата (см. {@link LoginAttemptService}).
     */
    @PostMapping("/register")
    public String register(@ModelAttribute("userDto") @Valid UserDto dto,
                           BindingResult result,
                           @AuthenticationPrincipal UserDetails userDetails,
                           HttpServletRequest request,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (userDetails != null) {
            return "redirect:/dashboard";
        }

        String clientIp = request.getRemoteAddr();
        if (loginAttemptService.isBlocked(clientIp)) {
            model.addAttribute("error", "Слишком много попыток регистрации с этого адреса. Попробуйте позже.");
            return "auth/register";
        }
        loginAttemptService.recordFailedAttempt(clientIp);

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