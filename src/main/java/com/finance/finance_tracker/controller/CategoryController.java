package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.UserService;
import com.finance.finance_tracker.util.SecurityUtil;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private final UserService userService;

    /**
     * Страница списка категорий.
     *
     * @param model модель представления
     * @return {@code "categories/list"}
     */
    @GetMapping
    public String categoriesPage(Model model) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<CategoryDto> categories = categoryService.getAllCategoriesByUserId(userId);
        model.addAttribute("categories", categories);
        return "categories/list";
    }

    /**
     * Страница формы создания категории.
     *
     * @param model модель представления
     * @return {@code "categories/create"}
     */
    @GetMapping("/create")
    public String newCategoryForm(Model model) {
        model.addAttribute("categoryDto", new CategoryDto());
        return "categories/create";
    }

    /**
     * Обрабатывает отправку формы создания категории — категория создаётся
     * для текущего аутентифицированного пользователя.
     *
     * @param dto                данные формы
     * @param result             результат валидации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @param userDetails        текущий пользователь
     * @return {@code "categories/create"} при ошибках валидации, иначе редирект
     */
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

    /**
     * Страница формы редактирования категории. Доступна только владельцу
     * категории (для стандартных категорий, у которых userId == null,
     * доступ открыт всем — это не ошибка, а норма).
     *
     * @param id    id категории
     * @param model модель представления
     * @return {@code "categories/edit"}
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если категория не найдена
     * @throws AccessDeniedException если категория принадлежит другому пользователю
     */
    @GetMapping("/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        CategoryDto category = categoryService.getCategoryById(id);
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Нет доступа к этой категории");
        }
        model.addAttribute("categoryDto", category);
        return "categories/edit";
    }

    /**
     * Обрабатывает отправку формы редактирования категории. Пустое имя и
     * уникальность нового имени проверяет Bean Validation
     * ({@code @NotBlank} + {@code @UniqueCategoryName} на {@link CategoryDto})
     * до вызова сервиса. Ошибки сервиса (default-категория, чужая категория)
     * перехватываются и отображаются как flash-сообщение.
     *
     * @param id                 id категории
     * @param dto                новые данные формы
     * @param result             результат валидации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /categories}, либо {@code "categories/edit"} при ошибках валидации
     */
    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("categoryDto") CategoryDto dto,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "categories/edit";
        }
        try {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            categoryService.updateCategory(id, dto.getName(), currentUserId);
            redirectAttributes.addFlashAttribute("success", "Категория обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    /**
     * Удаляет категорию. Ошибки (например, наличие связанных транзакций)
     * перехватываются и отображаются как flash-сообщение.
     *
     * @param id                 id категории
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на {@code /categories}
     */
    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            categoryService.deleteCategory(id, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Категория удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }
}
