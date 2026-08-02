package com.finance.finance_tracker.mapper;


import com.finance.finance_tracker.dto.TransactionDto;
import com.finance.finance_tracker.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountName", source = "account.name")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "account.currency", target = "accountCurrency")
    @Mapping(source = "category.id", target = "categoryId")
    TransactionDto toDto(Transaction transaction);
    Transaction toEntity(TransactionDto dto);
}
