package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.RoleDto;
import com.finance.finance_tracker.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role role);

    @Mapping(target = "id", ignore = true)
    Role toEntity(RoleDto roleDto);
}
