package com.finance.finance_tracker.mapper;


import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDto toDto(Account account);
    Account toEntity(AccountDto accountDto);
}
