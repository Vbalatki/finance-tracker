package com.finance.finance_tracker.controller;


import com.finance.finance_tracker.dto.ChangePasswordDto;
import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.dto.UserSettingsDto;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.Theme;
import com.finance.finance_tracker.service.UserService;
import com.finance.finance_tracker.service.impl.UserDetailsServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf-контроллер для страниц личного кабинета: просмотр и
 * редактирование профиля, смена пароля. Все методы работают с профилем
 * текущего аутентифицированного пользователя (берётся из
 * {@code @AuthenticationPrincipal}).
 */
@Controller
@RequestMapping("/profile")
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
    @GetMapping
    public String userProfile(Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
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
                                  @AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("userDto", user);
        return "users/edit";
    }

    /**
     * Обрабатывает отправку формы редактирования профиля. Если новый email
     * уже занят другим пользователем, ошибка приходит через
     * {@link com.finance.finance_tracker.validation.UniqueEmail} ещё на
     * этапе {@code @Valid} и попадает в {@code result} как ошибка поля
     * {@code email} — до тела этого метода такие случаи не доходят.
     *
     * @param dto                новые данные профиля
     * @param result             результат валидации
     * @param userDetails        текущий пользователь
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /profile} при успехе, иначе {@code "users/edit"}
     */
    @PostMapping("/edit")
    public String updateProfile(
            @ModelAttribute("userDto") @Valid UserDto dto,
            BindingResult result,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "users/edit";
        }

        try {
            UserDto currentUser = userService.getUserByEmail(userDetails.getUsername());
            UserDto updatedUser = userService.updateUser(currentUser.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
            return "redirect:/profile";
        } catch (Exception e) {
            result.rejectValue("email", "error.user", e.getMessage());
            return "users/edit";
        }
    }

    /**
     * Страница формы смены пароля.
     *
     * @param model модель представления
     * @return {@code "users/change-password"}
     */
    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "users/change-password";
    }

    /**
     * Обрабатывает отправку формы смены пароля. Совпадение нового пароля с
     * подтверждением и его минимальная длина проверяются Bean Validation
     * на {@link ChangePasswordDto} до вызова сервиса.
     *
     * @param dto                текущий пароль, новый пароль, подтверждение
     * @param result             результат валидации
     * @param userDetails        текущий пользователь
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /profile} при успехе, иначе {@code "users/change-password"}
     */
    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
            BindingResult result,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "users/change-password";
        }

        try {
            UserDto user = userService.getUserByEmail(userDetails.getUsername());
            userService.changePassword(user.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Пароль успешно изменен");
            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "users/change-password";
        }
    }

    @GetMapping("/profile/settings")
    public String settingsPage(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        model.addAttribute("settingsDto", userService.getUserSettings(user.getId()));
        model.addAttribute("currencies", Currency.values());
        return "users/settings";
    }

    /**
     * Мгновенное сохранение темы оформления — вызывается AJAX-запросом со
     * страницы настроек при клике на радио-кнопку, без формы и кнопки
     * "Сохранить". Читает текущие настройки, меняет только тему, сохраняет
     * целиком — переиспользует существующий bulk-метод сервиса, не плодит
     * отдельный узкий метод в сервисном слое ради одного поля.
     */
    @PostMapping("/profile/settings/theme")
    @ResponseBody
    public ResponseEntity<Void> updateTheme(@RequestParam Theme theme,
                                            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        UserSettingsDto settings = userService.getUserSettings(user.getId());
        settings.setTheme(theme);
        userService.updateUserSettings(user.getId(), settings);
        return ResponseEntity.ok().build();
    }

    /**
     * Мгновенное сохранение основной валюты — тот же приём, что и для темы.
     */
    @PostMapping("/profile/settings/currency")
    @ResponseBody
    public ResponseEntity<Void> updateDefaultCurrency(@RequestParam Currency defaultCurrency,
                                                      @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        UserSettingsDto settings = userService.getUserSettings(user.getId());
        settings.setDefaultCurrency(defaultCurrency);
        userService.updateUserSettings(user.getId(), settings);
        return ResponseEntity.ok().build();
    }
}
