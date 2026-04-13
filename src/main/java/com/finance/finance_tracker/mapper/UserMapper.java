package com.finance.finance_tracker.mapper;


import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(UserDto dto);
}
