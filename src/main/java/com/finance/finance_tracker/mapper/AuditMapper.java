package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditMapper {
    @Mapping(target = "id", ignore = true)
    public Audit toEntity(AuditDto dto);
    public AuditDto toDto(Audit entity);
}
