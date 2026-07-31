package com.finance.finance_tracker.mapper;


import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(source = "user.id", target = "userId")
    AccountDto toDto(Account account);
    @Mapping(target = "user", ignore = true)
    Account toEntity(AccountDto accountDto);
}
