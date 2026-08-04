package com.finance.finance_tracker.service;

import com.finance.finance_tracker.dto.CategoryDto;

import java.util.List;

/**
 * Управление категориями транзакций. Категория либо стандартная
 * (доступна всем пользователям на чтение, неизменяема), либо
 * пользовательская (принадлежит конкретному пользователю, полный CRUD
 * для владельца). Имя категории уникально в пределах видимости
 * пользователя (свои + стандартные), не только среди своих.
 */
public interface CategoryService {

    /**
     * Создаёт новую пользовательскую категорию.
     *
     * @param dto данные категории, {@code dto.userId} обязателен
     * @return созданная категория
     * @throws com.finance.finance_tracker.exception.InvalidDataException если имя пустое
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если пользователь не найден
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если имя уже занято своей или стандартной категорией
     */
    CategoryDto saveCategory(CategoryDto dto);

    /**
     * Возвращает категорию по id.
     *
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если категория не найдена
     */
    CategoryDto getCategoryById(Long id);

    /**
     * Возвращает категории, видимые пользователю: его собственные плюс
     * все стандартные.
     */
    List<CategoryDto> getAllCategoriesByUserId(Long userId);

    /**
     * Переименовывает пользовательскую категорию.
     *
     * @param id            id категории
     * @param name          новое имя
     * @param currentUserId id текущего аутентифицированного пользователя — проверяется владение
     * @throws com.finance.finance_tracker.exception.InvalidDataException если имя пустое или категория стандартная
     * @throws com.finance.finance_tracker.exception.AccessDeniedException если категория принадлежит другому пользователю
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если категория не найдена
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если новое имя уже занято
     */
    void updateCategory(Long id, String name, Long currentUserId);

    /**
     * Удаляет пользовательскую категорию. Категорию с уже привязанными
     * транзакциями или стандартную категорию удалить нельзя.
     *
     * @param id            id категории
     * @param currentUserId id текущего аутентифицированного пользователя — проверяется владение
     * @throws com.finance.finance_tracker.exception.InvalidDataException если категория стандартная или есть связанные транзакции
     * @throws com.finance.finance_tracker.exception.AccessDeniedException если категория принадлежит другому пользователю
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если категория не найдена
     */
    void deleteCategory(Long id, Long currentUserId);
}