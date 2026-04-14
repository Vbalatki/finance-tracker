package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(target = "transactionsCount", ignore = true) // заполняем в сервисе
    CategoryDto toDto(Category entity);


    @Mapping(target = "transactions", ignore = true)
    Category toEntity(CategoryDto dto);
}
