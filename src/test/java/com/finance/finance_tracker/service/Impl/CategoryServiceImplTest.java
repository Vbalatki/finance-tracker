package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.DuplicateEntityException;
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
        @DisplayName("создаёт категорию, когда имя уникально для пользователя")
        void saveCategory_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(categoryRepository.existsByNameAndUserId("Продукты", 1L)).thenReturn(false);
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
            when(categoryRepository.existsByNameAndUserId("  Продукты  ", 1L)).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);

            categoryService.saveCategory(categoryDto);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Продукты");
        }

        @Test
        @DisplayName("бросает InvalidDataException для пустого названия")
        void saveCategory_blankName_throws() {
            categoryDto.setName("   ");

            assertThrows(InvalidDataException.class, () -> categoryService.saveCategory(categoryDto));
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("бросает InvalidDataException для null названия")
        void saveCategory_nullName_throws() {
            categoryDto.setName(null);

            assertThrows(InvalidDataException.class, () -> categoryService.saveCategory(categoryDto));
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не найден")
        void saveCategory_userNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.saveCategory(categoryDto));
        }

        @Test
        @DisplayName("бросает DuplicateEntityException, если категория с таким именем уже есть")
        void saveCategory_duplicate_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(categoryRepository.existsByNameAndUserId("Продукты", 1L)).thenReturn(true);

            assertThrows(DuplicateEntityException.class, () -> categoryService.saveCategory(categoryDto));
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCategoryById / getAllCategories")
    class GetCategories {

        @Test
        @DisplayName("возвращает категорию по id")
        void getCategoryById_success() {
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);

            assertThat(categoryService.getCategoryById(5L)).isEqualTo(categoryDto);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если категория не найдена")
        void getCategoryById_notFound_throws() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.getCategoryById(404L));
        }

        @Test
        @DisplayName("возвращает все категории (без фильтра по пользователю)")
        void getAllCategories_returnsAll() {
            when(categoryRepository.findAllOrderById()).thenReturn(List.of(category));
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);

            List<CategoryDto> result = categoryService.getAllCategories();

            assertThat(result).containsExactly(categoryDto);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("обновляет название категории")
        void updateCategory_success() {
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameAndUserId("Новое имя", 1L)).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            categoryService.updateCategory(5L, "Новое имя");

            assertThat(category.getName()).isEqualTo("Новое имя");
        }

        @Test
        @DisplayName("не проверяет уникальность, если имя не меняется")
        void updateCategory_sameName_skipsUniquenessCheck() {
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            categoryService.updateCategory(5L, "Продукты");

            verify(categoryRepository, never()).existsByNameAndUserId(any(), any());
        }

        @Test
        @DisplayName("бросает InvalidDataException для пустого имени")
        void updateCategory_blankName_throws() {
            assertThrows(InvalidDataException.class, () -> categoryService.updateCategory(5L, "  "));
            verify(categoryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если категория не найдена")
        void updateCategory_notFound_throws() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.updateCategory(404L, "Имя"));
        }

        @Test
        @DisplayName("бросает DuplicateEntityException при переименовании на занятое имя")
        void updateCategory_duplicateName_throws() {
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameAndUserId("Занятое имя", 1L)).thenReturn(true);

            assertThrows(DuplicateEntityException.class, () -> categoryService.updateCategory(5L, "Занятое имя"));
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("удаляет категорию без транзакций")
        void deleteCategory_success() {
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

            categoryService.deleteCategory(5L);

            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если категория не найдена")
        void deleteCategory_notFound_throws() {
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> categoryService.deleteCategory(404L));
        }

        @Test
        @DisplayName("бросает InvalidDataException, если у категории есть транзакции")
        void deleteCategory_hasTransactions_throws() {
            category.setTransactions(List.of(new Transaction()));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

            assertThrows(InvalidDataException.class, () -> categoryService.deleteCategory(5L));
            verify(categoryRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getUserCategories")
    class GetUserCategories {

        @Test
        @DisplayName("возвращает категории конкретного пользователя")
        void getUserCategories_success() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(categoryRepository.findByUserId(1L)).thenReturn(List.of(category));
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);

            List<CategoryDto> result = categoryService.getUserCategories(1L);

            assertThat(result).containsExactly(categoryDto);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не существует")
        void getUserCategories_userNotFound_throws() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> categoryService.getUserCategories(99L));
            verify(categoryRepository, never()).findByUserId(any());
        }
    }
}
