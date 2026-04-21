package com.finance.finance_tracker.service;


import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountDto findById(Long id);
    AccountDto saveAccount(AccountDto dto);
    AccountDto deposit(Long accountId, BigDecimal amount);
    AccountDto withdraw(Long accountId, BigDecimal amount);
    List<AccountDto> getUserAccounts(Long userId);
    void deleteAccount(Long id);
    BigDecimal getTotalBalance(Long userId);
    BigDecimal getTotalBalanceInCurrency(Long userId, Currency currency);
    AccountDto updateAccount(Long id, AccountDto dto);
}
