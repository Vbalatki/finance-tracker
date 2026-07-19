package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.service.CategoryService;
import com.finance.finance_tracker.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link CategoryController} через standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CategoryController controller = new CategoryController(categoryService, userService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /categories возвращает список всех категорий")
    void categoriesPage_returnsListView() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(new CategoryDto()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/list"));
    }

    @Test
    @DisplayName("GET /categories/create возвращает форму создания")
    void newCategoryForm_returnsCreateView() throws Exception {
        mockMvc.perform(get("/categories/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/create"));
    }

    @Test
    @DisplayName("POST /categories с валидным именем создаёт категорию для текущего пользователя")
    void createCategory_valid_redirectsToCategories() throws Exception {
        UserDto currentUser = new UserDto();
        currentUser.setId(1L);
        when(userService.getUserByEmail("user@example.com")).thenReturn(currentUser);

        mockMvc.perform(post("/categories").param("name", "Развлечения"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).saveCategory(any(CategoryDto.class));
    }

    @Test
    @DisplayName("POST /categories с пустым именем возвращает форму с ошибками")
    void createCategory_blankName_returnsCreateView() throws Exception {
        mockMvc.perform(post("/categories").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/create"));

        verify(categoryService, never()).saveCategory(any());
    }

    @Test
    @DisplayName("POST /categories при дубликате имени редиректит обратно на форму с flash-ошибкой")
    void createCategory_duplicateName_redirectsWithFlashError() throws Exception {
        UserDto currentUser = new UserDto();
        currentUser.setId(1L);
        when(userService.getUserByEmail("user@example.com")).thenReturn(currentUser);
        doThrow(new DuplicateEntityException("Уже существует"))
                .when(categoryService).saveCategory(any(CategoryDto.class));

        mockMvc.perform(post("/categories").param("name", "Дубликат"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories/create"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /categories/{id}/edit возвращает форму редактирования")
    void editCategoryForm_returnsEditView() throws Exception {
        CategoryDto dto = new CategoryDto();
        dto.setId(5L);
        dto.setName("Продукты");
        when(categoryService.getCategoryById(5L)).thenReturn(dto);

        mockMvc.perform(get("/categories/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/edit"));
    }

    @Test
    @DisplayName("POST /categories/{id}/edit обновляет имя категории и редиректит")
    void updateCategory_success_redirects() throws Exception {
        mockMvc.perform(post("/categories/5/edit").param("name", "Новое имя"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"))
                .andExpect(flash().attributeExists("success"));

        verify(categoryService).updateCategory(5L, "Новое имя");
    }

    @Test
    @DisplayName("POST /categories/{id}/edit при ошибке сервиса пишет flash-ошибку")
    void updateCategory_serviceThrows_setsFlashError() throws Exception {
        doThrow(new RuntimeException("Категория не найдена"))
                .when(categoryService).updateCategory(404L, "Имя");

        mockMvc.perform(post("/categories/404/edit").param("name", "Имя"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /categories/{id}/delete удаляет категорию и редиректит")
    void deleteCategory_success_redirects() throws Exception {
        mockMvc.perform(post("/categories/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"))
                .andExpect(flash().attributeExists("success"));

        verify(categoryService).deleteCategory(5L);
    }

    @Test
    @DisplayName("POST /categories/{id}/delete при наличии транзакций пишет flash-ошибку")
    void deleteCategory_hasTransactions_setsFlashError() throws Exception {
        doThrow(new RuntimeException("Невозможно удалить категорию"))
                .when(categoryService).deleteCategory(5L);

        mockMvc.perform(post("/categories/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
}
