package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.RoleDto;
import com.finance.finance_tracker.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleMapperTest {

    private final RoleMapper mapper = new RoleMapperImpl();

    @Test
    @DisplayName("toDto переносит id/name, displayName вычисляется отдельно")
    void toDto_mapsFields() {
        Role role = new Role();
        role.setId(2L);
        role.setName("ROLE_ADMIN");

        RoleDto dto = mapper.toDto(role);
        assertThat(dto.getName()).isEqualTo("ROLE_ADMIN");
        assertThat(dto.getDisplayName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("toEntity игнорирует id из dto даже если он заполнен")
    void toEntity_ignoresIdFromDto() {
        RoleDto dto = new RoleDto();
        dto.setId(999L);
        dto.setName("ROLE_MANAGER");

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }
}
