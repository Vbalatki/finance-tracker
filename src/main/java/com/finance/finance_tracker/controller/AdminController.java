package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.DTO.RoleDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.service.AuditService;
import com.finance.finance_tracker.service.RoleService;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {
    private final RoleService roleService;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping
    public String dashboard(Model model) {
        List<UserDto> users = userService.getAllUsers();
        List<RoleDto> allRoles = roleService.findAll();
        List<AuditDto> recentLogs = auditService.getRecentLogs(50);

        model.addAttribute("users", users);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("newRole", new RoleDto());
        model.addAttribute("recentLogs", recentLogs);
        return "admin/dashboard";
    }

    @GetMapping("/audit")
    public String auditLogs(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "50") int size,
                            Model model) {
        var auditPage = auditService.getAuditLogs(page, size);
        model.addAttribute("logs", auditPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalElements", auditPage.getTotalElements());

        return "admin/audit";
    }

    @PostMapping("/roles/create")
    public String createRole(@Valid @ModelAttribute("newRole") RoleDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании роли");
        } else {
            try {
                roleService.create(dto);
                redirectAttributes.addFlashAttribute("success", "Роль создана!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/users/{userId}/roles")
    public String updateUserRoles(@PathVariable Long userId,
                                  @RequestParam(value = "roles", required = false) List<Long> roleIds,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.assignRoles(userId, roleIds != null ? roleIds : List.of());
            redirectAttributes.addFlashAttribute("success", "Роли пользователя обновлены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/users/{userId}/toggle")
    public String toggleUserActive(@PathVariable Long userId,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActive(userId);
            redirectAttributes.addFlashAttribute("success", "Статус пользователя изменён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("success", "Пользователь удалён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/roles/{roleId}/delete")
    public String deleteRole(@PathVariable Long roleId,
                             RedirectAttributes redirectAttributes) {
        try {
            roleService.delete(roleId);
            redirectAttributes.addFlashAttribute("success", "Роль удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}
