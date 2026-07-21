package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.Util.SecurityUtil;
import com.finance.finance_tracker.service.BudgetService;
import com.finance.finance_tracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
 * Thymeleaf-контроллер для страниц управления месячными бюджетами по категориям.
 */
@Controller
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;

    /**
     * Страница списка бюджетов текущего пользователя.
     *
     * @param userDetails текущий пользователь; {@code null}, если не аутентифицирован
     * @param model       модель представления
     * @return {@code "budgets/list"}, либо редирект на {@code /login}
     */
    @GetMapping
    public String listBudgets(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        Long userId = SecurityUtil.getCurrentUserId();

        List<BudgetDto> budgets = budgetService.getBudgetsByUserId(userId);
        model.addAttribute("budgets", budgets);
        return "budgets/list";
    }

    /**
     * Страница формы создания бюджета.
     *
     * @param categoryId предзаполняемая категория, необязателен
     * @param model      модель представления
     * @return {@code "budgets/form"}
     */
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false) Long categoryId, Model model) {
        List<CategoryDto> categories = categoryService.getAllCategories();
        BudgetDto budgetDto = new BudgetDto();
        if (categoryId != null) {
            budgetDto.setCategoryId(categoryId);
        }
        model.addAttribute("categories", categories);
        model.addAttribute("budgetDto", budgetDto);
        return "budgets/form";
    }

    /**
     * Обрабатывает отправку формы создания/обновления бюджета.
     *
     * @param budgetDto          данные формы
     * @param result             результат валидации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @param model              модель представления (при повторном рендере формы)
     * @return редирект на {@code /budgets}, либо {@code "budgets/form"} при ошибках валидации
     */
    @PostMapping
    public String saveBudget(@Valid @ModelAttribute("budgetDto") BudgetDto budgetDto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            result.getAllErrors().forEach(error -> System.out.println(error.getDefaultMessage()));
            return "budgets/form";
        }
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            budgetService.saveBudget(budgetDto, userId);
            redirectAttributes.addFlashAttribute("success", "Бюджет сохранён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budgets";
    }

    /**
     * Обнуляет накопленные траты бюджета. Ошибки перехватываются и
     * отображаются как flash-сообщение.
     *
     * @param id                 id бюджета
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /budgets}
     */
    @PostMapping("/{id}/reset")
    public String resetBudget(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            budgetService.resetSpending(id);
            redirectAttributes.addFlashAttribute("success", "Траты сброшены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budgets";
    }

    /**
     * Удаляет бюджет. Ошибки перехватываются и отображаются как flash-сообщение.
     *
     * @param id                 id бюджета
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /budgets}
     */
    @PostMapping("/{id}/delete")
    public String deleteBudget(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            budgetService.deleteBudget(id);
            redirectAttributes.addFlashAttribute("success", "Бюджет удалён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budgets";
    }
}
