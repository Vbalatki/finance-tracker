package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AccountDto;
import com.finance.finance_tracker.DTO.TransactionDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.mapper.AccountMapper;
import com.finance.finance_tracker.mapper.CategoryMapper;
import com.finance.finance_tracker.mapper.UserMapper;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.AccountService;
import com.finance.finance_tracker.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;


    @Transactional
    public UserDto registerUser(UserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("User with email: " + dto.getEmail() + " already registered");
        }

        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Transactional
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto getUserById(Long id) {
        User user = findById(id);
        return userMapper.toDto(user);
    }

    public List<UserDto> getAllUsers() {
        List<User> list = userRepository.findAll();
        return list.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto dto) {
        User user = findById(id);

        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setBirthday(dto.getBirthday());
        user.setEmail(dto.getEmail());

        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }


    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);

        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


    @Transactional
    public AccountDto addAccountToUser(Long userId, AccountDto accountDto) {
        User user = findById(userId);

        if (accountRepository.existsByNameAndUserId(accountDto.getName(), user.getId())) {
            throw new IllegalArgumentException("Account name must be unique for this user");
        }

        Account account = accountMapper.toEntity(accountDto);
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);

        Account savedAccount = accountRepository.save(account);
        user.getAccounts().add(savedAccount);

        return accountMapper.toDto(savedAccount);
    }



    @Transactional
    public List<AccountDto> getUserAccounts(Long userId) {
        User user = findById(userId);

        return user.getAccounts().stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }



    ////////////////////////////////////////////////////////////////////////////////

    public BigDecimal getUserTotalBalanceInRub(List<AccountDto> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalInRub = BigDecimal.ZERO;

        for (AccountDto dto : accounts) {
            BigDecimal balance = dto.getBalance();
            Currency currency = dto.getCurrency();

            totalInRub = totalInRub.add(convertToRub(balance, currency));
        }

        return totalInRub;
    }

    public BigDecimal getUserTotalIncomeInRub(List<TransactionDto> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TransactionDto t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                Currency currency = t.getAccountCurrency();
                total = total.add(convertToRub(t.getAmount(), currency));
            }
        }
        return total;
    }

    public BigDecimal getUserTotalExpenseInRub(List<TransactionDto> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TransactionDto t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                Currency currency = t.getAccountCurrency();
                total = total.add(convertToRub(t.getAmount(), currency));
            }
        }
        return total;
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }


    private BigDecimal convertToRub(BigDecimal amount, Currency currency) {
        if (currency == Currency.RUB) {
            return amount;
        }

        BigDecimal rate = currency.getRateToRub();
        return amount.multiply(rate);
    }
}
