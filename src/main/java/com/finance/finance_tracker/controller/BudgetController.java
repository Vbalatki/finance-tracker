package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.service.BudgetService;
import com.finance.finance_tracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;
    private final CategoryService categoryService;


    @GetMapping("/budgets")
    public String budgetsPage(Model model) {
        Long userId = 1L; // TODO: Получать из сессии

        LocalDate currentMonth = LocalDate.now();
        List<BudgetDto> budgets = budgetService.findByUserId(userId);
        List<CategoryDto> categoriesWithoutBudget = categoryService.findWithoutBudget(userId);

        BigDecimal totalLimit = budgets.stream()
                .map(BudgetDto::getMonthlyLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = budgets.stream()
                .map(BudgetDto::getCurrentSpending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = totalLimit.subtract(totalSpent);

        model.addAttribute("budgets", budgets);
        model.addAttribute("categoriesWithoutBudget", categoriesWithoutBudget);
        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("totalLimit", totalLimit);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("totalRemaining", totalRemaining);

        return "budgets/list";
    }

    @PostMapping("/budgets")
    public String createBudget(@ModelAttribute("budgetDto") @Valid BudgetDto dto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "budgets/list";
        }

        try {
            budgetService.createBudget(dto);
            redirectAttributes.addFlashAttribute("success", "Бюджет установлен!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budgets";
    }

    @PostMapping("/budgets/{id}/reset")
    public String resetBudget(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            budgetService.resetBudgetSpending(id);
            redirectAttributes.addFlashAttribute("success", "Траты сброшены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budgets";
    }

    @PostMapping("/budgets/{id}/delete")
    public String deleteBudget(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            budgetService.deleteBudget(id);
            redirectAttributes.addFlashAttribute("success", "Бюджет удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budgets";
    }
}