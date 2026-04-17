package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(Category category);
    @Mapping(target = "user", ignore = true)
    Category toEntity(CategoryDto dto);
}
