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

/**
 * Thymeleaf-контроллер админ-панели: управление пользователями, ролями и
 * просмотр журнала аудита. Доступ ограничен на уровне
 * {@code SecurityConfig} ({@code hasRole("ADMIN")} для {@code /admin/**}) —
 * сам контроллер прав не проверяет.
 */
@Controller
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {
    private final RoleService roleService;
    private final UserService userService;
    private final AuditService auditService;

    /**
     * Главная страница админ-панели: список пользователей, список ролей и
     * последние 50 записей аудита.
     *
     * @param model модель представления
     * @return {@code "admin/dashboard"}
     */
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

    /**
     * Страница полного журнала аудита с постраничной навигацией.
     *
     * @param page  номер страницы, начиная с 0
     * @param size  размер страницы
     * @param model модель представления
     * @return {@code "admin/audit"}
     */
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

    /**
     * Создаёт новую роль. Ошибки валидации и ошибки сервиса отображаются
     * как flash-сообщение после редиректа обратно на дашборд.
     *
     * @param dto                данные новой роли
     * @param result             результат валидации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /admin}
     */
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

    /**
     * Полностью заменяет набор ролей пользователя выбранными в форме.
     *
     * @param userId             id пользователя
     * @param roleIds            id выбранных ролей; {@code null}, если ни одна не выбрана
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /admin}
     */
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

    /**
     * Переключает статус активности пользователя (блокировка/разблокировка).
     *
     * @param userId             id пользователя
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /admin}
     */
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

    /**
     * Удаляет пользователя.
     *
     * @param userId             id пользователя
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /admin}
     */
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

    /**
     * Удаляет роль. Стандартные роли ({@code ROLE_ADMIN}/{@code ROLE_USER})
     * удалить нельзя — ошибка отображается как flash-сообщение.
     *
     * @param roleId             id роли
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /admin}
     */
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
