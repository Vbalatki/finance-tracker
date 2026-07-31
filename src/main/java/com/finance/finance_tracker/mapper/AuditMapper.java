package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditMapper {
    public Audit toEntity(AuditDto dto);
    public AuditDto toDto(Audit entity);
}
