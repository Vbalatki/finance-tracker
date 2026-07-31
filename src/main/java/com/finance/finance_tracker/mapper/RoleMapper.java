package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.RoleDto;
import com.finance.finance_tracker.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role role);
    Role toEntity(RoleDto roleDto);
}
