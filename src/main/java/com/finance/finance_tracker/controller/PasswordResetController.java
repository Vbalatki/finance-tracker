package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.ResetPasswordDto;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.service.PasswordResetService;
import com.finance.finance_tracker.service.impl.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final LoginAttemptService loginAttemptService;

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    /**
     * Сообщение об успехе всегда одинаковое, вне зависимости от того,
     * существует ли email в системе — иначе форма превращается в способ
     * проверить, кто зарегистрирован в трекере.
     */
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                 HttpServletRequest request,
                                 Model model) {
        String clientIp = request.getRemoteAddr();
        if (loginAttemptService.isBlocked(clientIp)) {
            model.addAttribute("error", "Слишком много попыток. Попробуйте позже.");
            return "auth/forgot-password";
        }
        loginAttemptService.recordFailedAttempt(clientIp);

        String resetLinkBase = buildBaseUrl(request) + "/reset-password";
        passwordResetService.requestReset(email, resetLinkBase);

        model.addAttribute("success",
                "Если такой email зарегистрирован, на него отправлена ссылка для сброса пароля.");
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        try {
            passwordResetService.validateToken(token);
        } catch (InvalidDataException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/reset-password-invalid";
        }
        model.addAttribute("token", token);
        model.addAttribute("resetPasswordDto", new ResetPasswordDto());
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @Valid @ModelAttribute("resetPasswordDto") ResetPasswordDto dto,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(token, dto.getNewPassword());
            redirectAttributes.addFlashAttribute("success", "Пароль успешно изменён. Войдите с новым паролем.");
            return "redirect:/login";
        } catch (InvalidDataException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/reset-password-invalid";
        }
    }

    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443);
        return scheme + "://" + host + (isDefaultPort ? "" : ":" + port);
    }
}