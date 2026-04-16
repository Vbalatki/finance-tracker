package com.finance.finance_tracker.mapper;


import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.Budget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BudgetMapper {
    @Mapping(target = "categoryName", source = "category.name")
    BudgetDto toDto(Budget budget);

    Budget toEntity(BudgetDto budgetDto);
}
