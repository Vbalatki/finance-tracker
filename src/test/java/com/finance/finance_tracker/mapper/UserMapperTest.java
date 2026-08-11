package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.entity.Role;
import com.finance.finance_tracker.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S6: до @Mapping(target = "id", ignore = true) на toEntity, подделанный
 * id в форме регистрации (/register — permitAll() в SecurityConfig, БЕЗ
 * аутентификации) заставлял Hibernate трактовать нового пользователя как
 * существующего и вместо persist() выполнять merge() поверх чужой строки.
 * См. UserRegistrationIdSpoofingIT для доказательства на реальной БД.
 */
class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    @DisplayName("toEntity игнорирует id из dto даже если он заполнен (S6)")
    void toEntity_ignoresIdFromDto() {
        UserDto dto = new UserDto();
        dto.setId(999L);
        dto.setName("Иван");
        dto.setSurname("Иванов");
        dto.setEmail("ivan@example.com");
        dto.setPassword("password123");

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }

    @Test
    @DisplayName("toDto мапит Set<Role> в Set<RoleDto> без явного uses-маппера")
    void toDto_mapsRolesSet() {
        Role role = new Role();
        role.setId(2L);
        role.setName("ROLE_ADMIN");

        User user = new User();
        user.setId(1L);
        user.setRoles(Set.of(role));

        UserDto dto = mapper.toDto(user);

        assertThat(dto.getRoles()).hasSize(1);
        assertThat(dto.getRoles().iterator().next().getName()).isEqualTo("ROLE_ADMIN");
    }
}
