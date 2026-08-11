package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private final AccountMapper mapper = new AccountMapperImpl();

    @Test
    @DisplayName("toDto подставляет user.id как userId")
    void toDto_mapsUserIdFromNestedUser() {
        User user = new User();
        user.setId(7L);

        Account account = new Account();
        account.setId(1L);
        account.setName("Основной счет");
        account.setBalance(new BigDecimal("500.00"));
        account.setCurrency(Currency.RUB);
        account.setUser(user);

        AccountDto dto = mapper.toDto(account);

        assertThat(dto.getUserId()).isEqualTo(7L);
        assertThat(dto.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("toEntity игнорирует id из dto даже если он заполнен (S5)")
    void toEntity_ignoresIdFromDto() {
        AccountDto dto = new AccountDto();
        dto.setId(999L);
        dto.setName("Новый счет");
        dto.setBalance(BigDecimal.TEN);
        dto.setCurrency(Currency.USD);
        dto.setUserId(1L);

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }
}
