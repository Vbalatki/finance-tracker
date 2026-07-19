package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.RoleDto;
import com.finance.finance_tracker.entity.Role;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.RoleMapper;
import com.finance.finance_tracker.repository.RoleRepository;
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
 * Unit-тесты для {@link RoleServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
    private RoleDto roleDto;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        role.setName("ROLE_MANAGER");

        roleDto = new RoleDto();
        roleDto.setName("MANAGER");
    }

    @Test
    @DisplayName("findAll возвращает все роли")
    void findAll_success() {
        when(roleRepository.findAllByOrderByIdAsc()).thenReturn(List.of(role));
        when(roleMapper.toDto(role)).thenReturn(roleDto);

        assertThat(roleService.findAll()).containsExactly(roleDto);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("создаёт роль с префиксом ROLE_ и именем в верхнем регистре")
        void create_success() {
            RoleDto dto = new RoleDto();
            dto.setName("manager");

            when(roleRepository.findByName("ROLE_MANAGER")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(roleMapper.toDto(any(Role.class))).thenReturn(roleDto);

            roleService.create(dto);

            ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
            verify(roleRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("ROLE_MANAGER");
        }

        @Test
        @DisplayName("бросает InvalidDataException для пустого имени")
        void create_blankName_throws() {
            RoleDto dto = new RoleDto();
            dto.setName("  ");

            assertThrows(InvalidDataException.class, () -> roleService.create(dto));
            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает DuplicateEntityException, если роль уже существует")
        void create_alreadyExists_throws() {
            RoleDto dto = new RoleDto();
            dto.setName("MANAGER");

            when(roleRepository.findByName("ROLE_MANAGER")).thenReturn(Optional.of(role));

            assertThrows(DuplicateEntityException.class, () -> roleService.create(dto));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("возвращает роль по id")
        void findById_success() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleMapper.toDto(role)).thenReturn(roleDto);

            assertThat(roleService.findById(1L)).isEqualTo(roleDto);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если роль не найдена")
        void findById_notFound_throws() {
            when(roleRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> roleService.findById(404L));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("обновляет имя роли")
        void update_success() {
            RoleDto dto = new RoleDto();
            dto.setName("newname");

            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleRepository.findByName("ROLE_NEWNAME")).thenReturn(Optional.empty());
            when(roleRepository.save(role)).thenReturn(role);

            roleService.update(1L, dto);

            assertThat(role.getName()).isEqualTo("ROLE_NEWNAME");
        }

        @Test
        @DisplayName("бросает InvalidDataException для пустого имени")
        void update_blankName_throws() {
            RoleDto dto = new RoleDto();
            dto.setName("");

            assertThrows(InvalidDataException.class, () -> roleService.update(1L, dto));
            verify(roleRepository, never()).findById(any());
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если роль не найдена")
        void update_notFound_throws() {
            RoleDto dto = new RoleDto();
            dto.setName("newname");

            when(roleRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> roleService.update(404L, dto));
        }

        @Test
        @DisplayName("бросает InvalidDataException при попытке изменить ROLE_ADMIN")
        void update_defaultAdminRole_throws() {
            role.setName("ROLE_ADMIN");
            RoleDto dto = new RoleDto();
            dto.setName("newname");

            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

            assertThrows(InvalidDataException.class, () -> roleService.update(1L, dto));
            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает DuplicateEntityException, если новое имя уже занято")
        void update_duplicateName_throws() {
            RoleDto dto = new RoleDto();
            dto.setName("existing");

            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleRepository.findByName("ROLE_EXISTING")).thenReturn(Optional.of(new Role()));

            assertThrows(DuplicateEntityException.class, () -> roleService.update(1L, dto));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("удаляет роль")
        void delete_success() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

            roleService.delete(1L);

            verify(roleRepository).delete(role);
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если роль не найдена")
        void delete_notFound_throws() {
            when(roleRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> roleService.delete(404L));
        }

        @Test
        @DisplayName("бросает InvalidDataException при попытке удалить ROLE_USER")
        void delete_defaultUserRole_throws() {
            role.setName("ROLE_USER");
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

            assertThrows(InvalidDataException.class, () -> roleService.delete(1L));
            verify(roleRepository, never()).delete(any(Role.class));
        }
    }
}
