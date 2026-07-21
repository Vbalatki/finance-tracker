package com.finance.finance_tracker.controller;


import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.service.Impl.UserDetailsServiceImpl;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf-контроллер для страниц личного кабинета: просмотр и
 * редактирование профиля, смена пароля. Все методы работают с профилем
 * текущего аутентифицированного пользователя (берётся из
 * {@code @AuthenticationPrincipal}).
 */
@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;


    /**
     * Страница профиля пользователя со списком его счетов.
     *
     * @param model       модель представления
     * @param userDetails текущий пользователь
     * @return {@code "users/profile"}
     */
    @GetMapping("/profile")
    public String userProfile(Model model,
                              @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        if (userDetails != null) {
            UserDto user = userService.getUserByEmail(userDetails.getUsername());
            model.addAttribute("user", user);
            model.addAttribute("accounts", userService.getUserAccounts(user.getId()));
        }
        return "users/profile";
    }

    /**
     * Страница формы редактирования профиля.
     *
     * @param model       модель представления
     * @param userDetails текущий пользователь
     * @return {@code "users/edit"}
     */
    @GetMapping("/edit")
    public String editProfilePage(Model model,
                                  @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("userDto", user);
        return "users/edit";
    }

    /**
     * Обрабатывает отправку формы редактирования профиля. Если новый email
     * уже занят другим пользователем, ошибка отображается как ошибка поля
     * {@code email} (не как flash-сообщение).
     *
     * @param dto                новые данные профиля
     * @param result             результат валидации
     * @param userDetails        текущий пользователь
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /users/profile} при успехе, иначе {@code "users/edit"}
     */
    @PostMapping("/edit")
    public String updateProfile(
            @ModelAttribute("userDto") @Valid UserDto dto,
            BindingResult result,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "users/edit";
        }

        try {
            UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
            UserDto updatedUser = userService.updateUser(currentUser.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
            return "redirect:/users/profile";
        } catch (Exception e) {
            result.rejectValue("email", "error.user", e.getMessage());
            return "users/edit";
        }
    }

    /**
     * Страница формы смены пароля.
     *
     * @return {@code "users/change-password"}
     */
    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "users/change-password";
    }

    /**
     * Обрабатывает отправку формы смены пароля. Совпадение нового пароля и
     * его подтверждения проверяется здесь же, до вызова сервиса.
     *
     * @param currentPassword    текущий пароль
     * @param newPassword        новый пароль
     * @param confirmPassword    подтверждение нового пароля
     * @param userDetails        текущий пользователь
     * @param model              модель представления (при повторном рендере формы)
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /users/profile} при успехе, иначе {@code "users/change-password"}
     */
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Проверка совпадения паролей
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Новый пароль и подтверждение не совпадают");
            return "users/change-password";
        }

        try {
            UserDto user = userService.getUserByEmail(userDetails.getUsername());
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Пароль успешно изменен");
            return "redirect:/users/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "users/change-password";
        }
    }
}
