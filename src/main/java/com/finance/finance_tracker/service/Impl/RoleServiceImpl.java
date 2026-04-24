package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.RoleDto;
import com.finance.finance_tracker.entity.Role;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.RoleMapper;
import com.finance.finance_tracker.repository.RoleRepository;
import com.finance.finance_tracker.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.Util.DataConstants.CANNOT_DELETE_DEFAULT_ROLE;
import static com.finance.finance_tracker.Util.DataConstants.CANNOT_MODIFY_DEFAULT_ROLE;
import static com.finance.finance_tracker.Util.DataConstants.DEFAULT_ROLE_ADMIN;
import static com.finance.finance_tracker.Util.DataConstants.DEFAULT_ROLE_USER;
import static com.finance.finance_tracker.Util.DataConstants.ROLE_ALREADY_EXISTS;
import static com.finance.finance_tracker.Util.DataConstants.ROLE_NAME_BLANK;
import static com.finance.finance_tracker.Util.DataConstants.ROLE_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.ROLE_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;


    @Transactional(readOnly = true)
    public List<RoleDto> findAll() {
        log.debug("Запрос всех ролей");

        List<Role> roles = roleRepository.findAllByOrderByIdAsc();

        log.debug("Найдено ролей: {}", roles.size());

        return roles.stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleDto create(RoleDto dto) {
        log.debug("Создание новой роли: name={}", dto.getName());

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new InvalidDataException(ROLE_NAME_BLANK);
        }

        String roleName = ROLE_PREFIX + dto.getName().toUpperCase().trim();

        if (roleRepository.findByName(roleName).isPresent()) {
            log.warn("Попытка создать уже существующую роль: {}", roleName);
            throw new DuplicateEntityException(new StringBuilder().append(ROLE_ALREADY_EXISTS).append(": ").append(roleName).toString());
        }

        Role role = new Role();
        role.setName(roleName);

        Role savedRole = roleRepository.save(role);
        log.info("Создана новая роль: id={}, name={}", savedRole.getId(), savedRole.getName());

        return roleMapper.toDto(savedRole);
    }

    @Transactional(readOnly = true)
    public RoleDto findById(Long id) {
        log.debug("Поиск роли по id: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Роль не найдена: id={}", id);
                    return new EntityNotFoundException(ROLE_NOT_FOUND + ", id: " + id);
                });

        return roleMapper.toDto(role);
    }

    @Transactional
    public void update(Long id, RoleDto dto) {
        log.debug("Обновление роли: id={}, newName={}", id, dto.getName());

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new InvalidDataException(ROLE_NAME_BLANK);
        }

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Роль не найдена при обновлении: id={}", id);
                    return new EntityNotFoundException(ROLE_NOT_FOUND + ", id: " + id);
                });

        if (DEFAULT_ROLE_ADMIN.equals(role.getName()) || DEFAULT_ROLE_USER.equals(role.getName())) {
            log.warn("Попытка изменить стандартную роль: id={}, name={}", id, role.getName());
            throw new InvalidDataException(CANNOT_MODIFY_DEFAULT_ROLE);
        }

        String oldName = role.getName();
        String newName = ROLE_PREFIX + dto.getName().toUpperCase().trim();

        if (!oldName.equals(newName) && roleRepository.findByName(newName).isPresent()) {
            log.warn("Попытка обновить роль на уже существующее имя: id={}, newName={}", id, newName);
            throw new DuplicateEntityException(ROLE_ALREADY_EXISTS + ": " + newName);
        }

        role.setName(newName);
        roleRepository.save(role);

        log.info("Обновлена роль: id={}, oldName={}, newName={}", id, oldName, newName);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Удаление роли: id={}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Роль не найдена при удалении: id={}", id);
                    return new EntityNotFoundException(ROLE_NOT_FOUND + ", id: " + id);
                });

        if (DEFAULT_ROLE_ADMIN.equals(role.getName()) || DEFAULT_ROLE_USER.equals(role.getName())) {
            log.warn("Попытка удалить стандартную роль: id={}, name={}", id, role.getName());
            throw new InvalidDataException(CANNOT_DELETE_DEFAULT_ROLE);
        }

        String roleName = role.getName();
        roleRepository.delete(role);

        log.info("Удалена роль: id={}, name={}", id, roleName);
    }
}