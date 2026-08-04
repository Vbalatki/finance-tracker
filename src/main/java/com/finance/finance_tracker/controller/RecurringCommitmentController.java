package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.RecurringCommitmentDto;
import com.finance.finance_tracker.util.SecurityUtil;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.RecurringCommitmentService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recurring")
public class RecurringCommitmentController {

    private final RecurringCommitmentService recurringCommitmentService;
    private final CategoryService categoryService;
    private final AccountService accountService;

    @GetMapping
    public String list(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        model.addAttribute("commitments", recurringCommitmentService.getByUserId(userId));
        return "recurring/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        model.addAttribute("commitmentDto", new RecurringCommitmentDto());
        model.addAttribute("categories", categoryService.getAllCategoriesByUserId(userId));
        model.addAttribute("accounts", accountService.getUserAccounts(userId));
        model.addAttribute("types", TransactionType.values());
        return "recurring/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("commitmentDto") RecurringCommitmentDto dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategoriesByUserId(userId));
            model.addAttribute("accounts", accountService.getUserAccounts(userId));
            model.addAttribute("types", TransactionType.values());
            return "recurring/form";
        }
        try {
            recurringCommitmentService.save(dto, userId);
            redirectAttributes.addFlashAttribute("success", "Плановое списание сохранено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recurring";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            recurringCommitmentService.toggleActive(id, SecurityUtil.getCurrentUserId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recurring";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            recurringCommitmentService.delete(id, SecurityUtil.getCurrentUserId());
            redirectAttributes.addFlashAttribute("success", "Плановое списание удалено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recurring";
    }
}