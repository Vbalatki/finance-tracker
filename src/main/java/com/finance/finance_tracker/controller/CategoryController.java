package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping
    public String categoriesPage(Model model) {
        List<CategoryDto> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "categories/list";
    }

    @GetMapping("/create")
    public String newCategoryForm(Model model) {
        model.addAttribute("categoryDto", new CategoryDto());
        return "categories/create";
    }

    @PostMapping
    public String createCategory(@Valid @ModelAttribute("categoryDto") CategoryDto dto,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        if (result.hasErrors()) {
            return "categories/create";
        }
        try {
            UserDto userDto = userService.getUserByEmail(userDetails.getUsername());
            dto.setUserId(userDto.getId());
            categoryService.saveCategory(dto);
            redirectAttributes.addFlashAttribute("success", "Категория создана!");
            return "redirect:/categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        CategoryDto category = categoryService.getCategoryById(id);
        model.addAttribute("categoryDto", category);
        return "categories/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam String name,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, name);
            redirectAttributes.addFlashAttribute("success", "Категория обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Категория удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }
}