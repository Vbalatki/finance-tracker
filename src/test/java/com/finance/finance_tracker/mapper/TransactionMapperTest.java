package com.finance.finance_tracker.mapper;

import com.finance.finance_tracker.dto.TransactionDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapperImpl();

    @Test
    @DisplayName("toDto подставляет account/category поля из вложенных сущностей")
    void toDto_mapsNestedAccountAndCategory() {
        Account account = new Account();
        account.setId(10L);
        account.setName("Основной счет");
        account.setCurrency(Currency.RUB);

        Category category = new Category();
        category.setId(5L);
        category.setName("Продукты");

        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setType(TransactionType.EXPENSE);
        tx.setAccount(account);
        tx.setCategory(category);

        TransactionDto dto = mapper.toDto(tx);

        assertThat(dto.getAccountId()).isEqualTo(10L);
        assertThat(dto.getAccountName()).isEqualTo("Основной счет");
        assertThat(dto.getCategoryId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("toDto без категории не бросает NPE")
    void toDto_nullCategory_doesNotThrow() {
        Account account = new Account();
        account.setId(10L);
        account.setCurrency(Currency.RUB);

        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setAmount(BigDecimal.TEN);
        tx.setType(TransactionType.INCOME);
        tx.setAccount(account);

        assertThat(mapper.toDto(tx).getCategoryId()).isNull();
    }

    @Test
    @DisplayName("toEntity игнорирует id из dto (S5) — единственный toEntity на реальном create-пути saveTransaction")
    void toEntity_ignoresIdFromDto() {
        TransactionDto dto = new TransactionDto();
        dto.setId(999L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setType(TransactionType.EXPENSE);
        dto.setAccountId(10L);

        assertThat(mapper.toEntity(dto).getId()).isNull();
    }
}
