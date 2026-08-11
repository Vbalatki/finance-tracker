package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.RecurringCommitmentDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.RecurringCommitment;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringCommitmentMapperTest {

    private final RecurringCommitmentMapper mapper = new RecurringCommitmentMapperImpl();

    @Test
    @DisplayName("toDto подставляет category/account/user поля из вложенных сущностей")
    void toDto_mapsNestedEntities() {
        User user = new User();
        user.setId(1L);
        Category category = new Category();
        category.setId(5L);
        category.setName("Развлечения");
        Account account = new Account();
        account.setId(10L);
        account.setName("Основной счет");

        RecurringCommitment commitment = new RecurringCommitment();
        commitment.setId(1L);
        commitment.setName("Netflix");
        commitment.setAmount(new BigDecimal("799.00"));
        commitment.setType(TransactionType.EXPENSE);
        commitment.setDayOfMonth(15);
        commitment.setUser(user);
        commitment.setCategory(category);
        commitment.setAccount(account);

        RecurringCommitmentDto dto = mapper.toDto(commitment);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getCategoryName()).isEqualTo("Развлечения");
        assertThat(dto.getAccountName()).isEqualTo("Основной счет");
    }

    @Test
    @DisplayName("toEntity игнорирует id из dto")
    void toEntity_ignoresIdFromDto() {
        RecurringCommitmentDto dto = new RecurringCommitmentDto();
        dto.setId(999L);
        dto.setName("Netflix");
        dto.setAmount(BigDecimal.TEN);
        dto.setType(TransactionType.EXPENSE);
        dto.setDayOfMonth(1);

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }
}
