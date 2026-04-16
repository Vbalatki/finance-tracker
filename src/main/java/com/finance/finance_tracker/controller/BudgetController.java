package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.service.BudgetService;
import com.finance.finance_tracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;

    @GetMapping
    public String listBudgets(Model model) {
        Long userId = 1L;
        List<BudgetDto> budgets = budgetService.getBudgetsByUserId(userId);
        model.addAttribute("budgets", budgets);
        return "budgets/list";
    }

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
            Long userId = 1L;
            budgetService.saveBudget(budgetDto, userId);
            redirectAttributes.addFlashAttribute("success", "Бюджет сохранён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budgets";
    }

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