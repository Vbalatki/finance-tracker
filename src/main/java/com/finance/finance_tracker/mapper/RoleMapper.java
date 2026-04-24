package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.DTO.RoleDto;
import com.finance.finance_tracker.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role role);
    Role toEntity(RoleDto roleDto);
}
