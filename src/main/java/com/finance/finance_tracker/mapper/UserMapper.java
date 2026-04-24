package com.finance.finance_tracker.mapper;


import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "roles", source = "roles")
    UserDto toDto(User user);
    User toEntity(UserDto dto);
}
