package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.CategoryDto;

import java.util.List;

/**
 * Управление категориями транзакций (например, "Продукты", "Транспорт").
 * Имя категории уникально в пределах одного пользователя.
 */
public interface CategoryService {

    /**
     * Создаёт новую категорию.
     *
     * @param dto данные категории, {@code dto.userId} обязателен
     * @return созданная категория
     * @throws com.finance.finance_tracker.exception.InvalidDataException если имя пустое
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если у пользователя уже есть категория с таким именем
     */
    CategoryDto saveCategory(CategoryDto dto);

    /**
     * Возвращает категорию по id.
     *
     * @param id id категории
     * @return DTO категории
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если категория не найдена
     */
    CategoryDto getCategoryById(Long id);

    /**
     * Возвращает все категории системы, без фильтрации по пользователю.
     *
     * <p><b>Внимание:</b> этот метод отдаёт категории всех пользователей,
     * а не только текущего — при использовании в контроллерах, доступных
     * рядовому пользователю, это приводит к утечке чужих категорий в UI.
     * В {@link com.finance.finance_tracker.service.Impl.CategoryServiceImpl}
     * есть метод {@code getUserCategories(Long)} для категорий конкретного
     * пользователя, но он не объявлен в этом интерфейсе и в контроллерах
     * сейчас не используется.
     *
     * @return список всех категорий, отсортированный по id
     */
    List<CategoryDto> getAllCategories();

    /**
     * Переименовывает категорию.
     *
     * @param id   id категории
     * @param name новое имя
     * @throws com.finance.finance_tracker.exception.InvalidDataException если имя пустое
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если категория не найдена
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если новое имя уже занято другой категорией того же пользователя
     */
    void updateCategory(Long id, String name);

    /**
     * Удаляет категорию. Категорию с уже привязанными транзакциями удалить нельзя.
     *
     * @param id id категории
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если категория не найдена
     * @throws com.finance.finance_tracker.exception.InvalidDataException если у категории есть связанные транзакции
     */
    void deleteCategory(Long id);
}
