package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.CategoryMapper;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link CategoryServiceImpl}.
 * saveCategory_blankName_throws / saveCategory_nullName_throws /
 * saveCategory_duplicate_throws / updateCategory_blankName_throws /
 * updateCategory_duplicateName_throws / updateCategory_sameName_skipsUniquenessCheck
 * удалены — эта логика переехала в @NotBlank + @UniqueCategoryName на
 * CategoryDto, тестируется отдельно в UniqueCategoryNameValidatorTest.
 *
 * Nested-класс "getUserCategories" удалён вместе с самим методом
 * CategoryServiceImpl.getUserCategories — метод не был задекларирован в
 * интерфейсе CategoryService (лишний публичный метод прямо на классе),
 * ни один контроллер его не вызывал (CategoryController/BudgetController/
 * RecurringCommitmentController/TransactionController используют только
 * getAllCategoriesByUserId). См. AUDIT_LOG.md, запись про мёртвый код.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User user;
    private Category category;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        category = new Category();
        category.setId(5L);
        category.setName("Продукты");
        category.setUser(user);

        categoryDto = new CategoryDto();
        categoryDto.setName("Продукты");
        categoryDto.setUserId(1L);
    }

    @Nested
    @DisplayName("saveCategory")
    class SaveCategory {

        @Test
        @DisplayName("создаёт категорию для существующего пользователя")
        void saveCategory_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);

            CategoryDto result = categoryService.saveCategory(categoryDto);

            assertThat(result).isEqualTo(categoryDto);
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Продукты");
            assertThat(captor.getValue().getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("обрезает пробелы в названии перед сохранением")
        void saveCategory_trimsName() {
            categoryDto.setName("  Продукты  ");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);

            categoryService.saveCategory(categoryDto);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Продукты");
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не найден")
        void saveCategory_userNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.saveCategory(categoryDto));
        }
    }

    @Nested
    @DisplayName("getAllCategoriesByUserId")
    class GetAllCategoriesByUserId {

        @Test
        @DisplayName("возвращает и собственные, и стандартные категории")
        void getAllCategoriesByUserId_returnsOwnAndDefault() {
            Category defaultCategory = new Category();
            defaultCategory.setId(1L);
            defaultCategory.setName("Транспорт");

            when(categoryRepository.findVisibleToUserOrderByIdAsc(1L))
                    .thenReturn(List.of(category, defaultCategory));
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);
            CategoryDto defaultDto = new CategoryDto();
            defaultDto.setDefaultCategory(true);
            when(categoryMapper.toDto(defaultCategory)).thenReturn(defaultDto);

            List<CategoryDto> result = categoryService.getAllCategoriesByUserId(1L);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("обновляет название категории её владельцем")
        void updateCategory_success() {
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            categoryService.updateCategory(5L, "Новое имя", 1L);

            assertThat(category.getName()).isEqualTo("Новое имя");
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если категория не найдена")
        void updateCategory_notFound_throws() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.updateCategory(404L, "Имя", 1L));
        }

        @Test
        @DisplayName("бросает InvalidDataException при попытке изменить стандартную категорию")
        void updateCategory_defaultCategory_throws() {
            Category defaultCategory = new Category();
            defaultCategory.setId(2L);
            defaultCategory.setName("Транспорт");

            when(categoryRepository.findById(2L)).thenReturn(Optional.of(defaultCategory));

            assertThrows(InvalidDataException.class, () -> categoryService.updateCategory(2L, "Новое имя", 1L));
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает AccessDeniedException при попытке изменить чужую категорию")
        void updateCategory_notOwner_throws() {
            User otherUser = new User();
            otherUser.setId(999L);
            category.setUser(otherUser);

            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

            assertThrows(AccessDeniedException.class, () -> categoryService.updateCategory(5L, "Новое имя", 1L));
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("удаляет категорию без транзакций её владельцем")
        void deleteCategory_success() {
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

            categoryService.deleteCategory(5L, 1L);

            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если категория не найдена")
        void deleteCategory_notFound_throws() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.deleteCategory(404L, 1L));
        }

        @Test
        @DisplayName("бросает InvalidDataException, если у категории есть транзакции")
        void deleteCategory_hasTransactions_throws() {
            category.setTransactions(List.of(new Transaction()));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

            assertThrows(InvalidDataException.class, () -> categoryService.deleteCategory(5L, 1L));
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("бросает InvalidDataException при попытке удалить стандартную категорию")
        void deleteCategory_defaultCategory_throws() {
            Category defaultCategory = new Category();
            defaultCategory.setId(2L);
            defaultCategory.setName("Транспорт");

            when(categoryRepository.findById(2L)).thenReturn(Optional.of(defaultCategory));

            assertThrows(InvalidDataException.class, () -> categoryService.deleteCategory(2L, 1L));
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("бросает AccessDeniedException при попытке удалить чужую категорию")
        void deleteCategory_notOwner_throws() {
            User otherUser = new User();
            otherUser.setId(999L);
            category.setUser(otherUser);

            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

            assertThrows(AccessDeniedException.class, () -> categoryService.deleteCategory(5L, 1L));
            verify(categoryRepository, never()).delete(any());
        }
    }
}