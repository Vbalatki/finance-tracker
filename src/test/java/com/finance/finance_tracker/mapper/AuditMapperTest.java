package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMapperTest {

    private final AuditMapper mapper = new AuditMapperImpl();

    @Test
    @DisplayName("toDto переносит все поля один в один")
    void toDto_mapsAllFields() {
        Audit audit = new Audit();
        audit.setAction("CREATE");
        audit.setEntityType("Account");
        audit.setUsername("ivan@example.com");

        AuditDto dto = mapper.toDto(audit);
        assertThat(dto.getAction()).isEqualTo("CREATE");
        assertThat(dto.getUsername()).isEqualTo("ivan@example.com");
    }

    @Test
    @DisplayName("toEntity игнорирует id из dto")
    void toEntity_ignoresIdFromDto() {
        AuditDto dto = new AuditDto();
        dto.setId(999L);
        dto.setAction("CREATE");

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }
}
