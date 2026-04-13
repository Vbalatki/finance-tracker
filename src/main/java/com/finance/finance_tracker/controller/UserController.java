package com.finance.finance_tracker.controller;


import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.service.Impl.UserDetailsServiceImpl;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;


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

    // Страница редактирования профиля
    @GetMapping("/edit")
    public String editProfilePage(Model model,
                                  @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UserDto user = userService.getUserByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("userDto", user);
        return "users/edit";
    }

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

    // Страница изменения пароля
    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "users/change-password";
    }

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