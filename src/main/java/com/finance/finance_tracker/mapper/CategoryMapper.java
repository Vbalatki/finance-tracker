package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.CategoryDto;
import com.finance.finance_tracker.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "defaultCategory", source = "defaultCategory")
    CategoryDto toDto(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Category toEntity(CategoryDto dto);
}