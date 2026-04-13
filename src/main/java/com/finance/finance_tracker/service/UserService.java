package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.CategoryDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface UserService {
    UserDto registerUser(UserDto dto);
    UserDto getUserById(Long id);
    List<UserDto> getAllUsers();
    UserDto updateUser(Long id, UserDto dto);
    void deleteUser(Long id);
    void changePassword(Long userId, String currentPassword, String newPassword);
    UserDto getUserByEmail(String email);
    AccountDto addAccountToUser(Long userId, AccountDto dto);
    List<AccountDto> getUserAccounts(Long userId);
    BigDecimal getUserTotalBalanceInRub(List<AccountDto> list);
    BigDecimal getUserTotalIncomeInRub(List<TransactionDto> list);
    BigDecimal getUserTotalExpenseInRub(List<TransactionDto> list);
}
