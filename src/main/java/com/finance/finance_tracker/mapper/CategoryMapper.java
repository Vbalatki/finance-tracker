package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(Category entity);
    Category toEntity(CategoryDto dto);
}
