package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.CategoryMapper;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.util.DataConstants.CANNOT_DELETE_CATEGORY;
import static com.finance.finance_tracker.util.DataConstants.CANNOT_DELETE_DEFAULT_CATEGORY;
import static com.finance.finance_tracker.util.DataConstants.CANNOT_MODIFY_DEFAULT_CATEGORY;
import static com.finance.finance_tracker.util.DataConstants.CATEGORY_NAME_BLANK;
import static com.finance.finance_tracker.util.DataConstants.CATEGORY_NAME_EXISTS;
import static com.finance.finance_tracker.util.DataConstants.CATEGORY_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CategoryDto saveCategory(CategoryDto dto) {
        log.debug("Сохранение новой категории: name={}, userId={}", dto.getName(), dto.getUserId());

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new InvalidDataException(CATEGORY_NAME_BLANK);
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> {
                    log.error("Пользователь не найден при сохранении категории: userId={}", dto.getUserId());
                    return new EntityNotFoundException(USER_NOT_FOUND + ", id: " + dto.getUserId());
                });

        if (categoryRepository.existsByNameVisibleToUser(dto.getName(), dto.getUserId())) {
            log.warn("Попытка создать дубликат категории: name={}, userId={}", dto.getName(), dto.getUserId());
            throw new DuplicateEntityException(CATEGORY_NAME_EXISTS + ": " + dto.getName());
        }

        Category category = new Category();
        category.setName(dto.getName().trim());
        category.setUser(user);

        Category savedCategory = categoryRepository.save(category);
        log.info("Создана новая категория: id={}, name={}, userId={}",
                savedCategory.getId(), savedCategory.getName(), user.getId());

        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        log.debug("Поиск категории по id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Категория не найдена: id={}", id);
                    return new EntityNotFoundException(CATEGORY_NOT_FOUND + ", id: " + id);
                });

        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategoriesByUserId(Long userId) {
        log.debug("Запрос категорий, видимых пользователю: userId={}", userId);

        List<Category> categories = categoryRepository.findVisibleToUserOrderByIdAsc(userId);

        log.debug("Найдено категорий: {}", categories.size());

        return categories.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateCategory(Long id, String name, Long currentUserId) {
        log.debug("Обновление категории: id={}, newName={}, currentUserId={}", id, name, currentUserId);

        if (name == null || name.isBlank()) {
            throw new InvalidDataException(CATEGORY_NAME_BLANK);
        }

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Категория не найдена при обновлении: id={}", id);
                    return new EntityNotFoundException(CATEGORY_NOT_FOUND + ", id: " + id);
                });

        // Стандартную категорию проверяем ДО владения — иначе
        // category.getUser().getId() ниже бросил бы NullPointerException.
        if (category.isDefaultCategory()) {
            log.warn("Попытка изменить стандартную категорию: id={}, name={}", id, category.getName());
            throw new InvalidDataException(CANNOT_MODIFY_DEFAULT_CATEGORY);
        }

        // ИСПРАВЛЕНО: раньше здесь не было проверки владения — любой
        // аутентифицированный пользователь мог переименовать чужую
        // категорию по id (IDOR).
        if (!category.getUser().getId().equals(currentUserId)) {
            log.warn("Попытка изменить чужую категорию: id={}, currentUserId={}, ownerId={}",
                    id, currentUserId, category.getUser().getId());
            throw new AccessDeniedException("Нет доступа к этой категории");
        }

        if (!category.getName().equals(name)) {
            if (categoryRepository.existsByNameVisibleToUser(name, currentUserId)) {
                log.warn("Попытка обновить категорию на уже существующее имя: id={}, name={}", id, name);
                throw new DuplicateEntityException(CATEGORY_NAME_EXISTS + ": " + name);
            }
        }

        String oldName = category.getName();
        category.setName(name.trim());
        categoryRepository.save(category);

        log.info("Обновлена категория: id={}, oldName={}, newName={}", id, oldName, name);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, Long currentUserId) {
        log.debug("Удаление категории: id={}, currentUserId={}", id, currentUserId);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Категория не найдена при удалении: id={}", id);
                    return new EntityNotFoundException(CATEGORY_NOT_FOUND + ", id: " + id);
                });

        if (category.isDefaultCategory()) {
            log.warn("Попытка удалить стандартную категорию: id={}, name={}", id, category.getName());
            throw new InvalidDataException(CANNOT_DELETE_DEFAULT_CATEGORY);
        }

        if (!category.getUser().getId().equals(currentUserId)) {
            log.warn("Попытка удалить чужую категорию: id={}, currentUserId={}, ownerId={}",
                    id, currentUserId, category.getUser().getId());
            throw new AccessDeniedException("Нет доступа к этой категории");
        }

        if (category.getTransactions() != null && !category.getTransactions().isEmpty()) {
            log.warn("Попытка удалить категорию с транзакциями: id={}, transactionsCount={}",
                    id, category.getTransactions().size());
            throw new InvalidDataException(CANNOT_DELETE_CATEGORY);
        }

        categoryRepository.delete(category);
        log.info("Удалена категория: id={}, name={}", id, category.getName());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getUserCategories(Long userId) {
        log.debug("Запрос категорий для пользователя: userId={}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("Пользователь не найден при запросе категорий: userId={}", userId);
            throw new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId);
        }

        List<Category> categories = categoryRepository.findByUserId(userId);

        log.debug("Найдено категорий для пользователя {}: {}", userId, categories.size());

        return categories.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }
}