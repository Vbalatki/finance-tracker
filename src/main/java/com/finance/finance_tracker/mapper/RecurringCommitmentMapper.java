package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.RecurringCommitmentDto;
import com.finance.finance_tracker.entity.RecurringCommitment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecurringCommitmentMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountName", source = "account.name")
    @Mapping(target = "userId", source = "user.id")
    RecurringCommitmentDto toDto(RecurringCommitment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "account", ignore = true)
    RecurringCommitment toEntity(RecurringCommitmentDto dto);
}
