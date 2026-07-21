package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.RoleDto;

import java.util.List;

/**
 * Управление ролями пользователей (например, {@code ROLE_ADMIN}, {@code ROLE_USER}).
 * Роли {@code ROLE_ADMIN} и {@code ROLE_USER} — стандартные и защищены от
 * изменения/удаления.
 */
public interface RoleService {

    /**
     * Возвращает все роли, отсортированные по id.
     *
     * @return список ролей
     */
    List<RoleDto> findAll();

    /**
     * Создаёт новую роль. Имя автоматически переводится в верхний регистр
     * и дополняется префиксом {@code ROLE_}, если его ещё нет.
     *
     * @param dto данные роли (используется только имя)
     * @return созданная роль
     * @throws com.finance.finance_tracker.exception.InvalidDataException если имя пустое
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если роль с таким именем уже существует
     */
    RoleDto create(RoleDto dto);

    /**
     * Удаляет роль.
     *
     * @param id id роли
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если роль не найдена
     * @throws com.finance.finance_tracker.exception.InvalidDataException если это стандартная роль ({@code ROLE_ADMIN}/{@code ROLE_USER})
     */
    void delete(Long id);

    /**
     * Возвращает роль по id.
     *
     * @param id id роли
     * @return DTO роли
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если роль не найдена
     */
    RoleDto findById(Long id);

    /**
     * Переименовывает роль (с тем же приведением имени, что и {@link #create(RoleDto)}).
     *
     * @param id  id роли
     * @param dto новое имя роли
     * @throws com.finance.finance_tracker.exception.EntityNotFoundException если роль не найдена
     * @throws com.finance.finance_tracker.exception.InvalidDataException если имя пустое или это стандартная роль
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException если новое имя уже занято другой ролью
     */
    void update(Long id, RoleDto dto);
}
