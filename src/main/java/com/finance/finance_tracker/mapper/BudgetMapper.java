package com.finance.finance_tracker.mapper;


import com.finance.finance_tracker.DTO.BudgetDto;
import com.finance.finance_tracker.entity.Budget;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BudgetMapper {
    BudgetDto toDto(Budget budget);
    Budget toEntity(BudgetDto budgetDto);
}
