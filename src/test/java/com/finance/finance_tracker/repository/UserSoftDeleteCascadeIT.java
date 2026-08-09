package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Transaction;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import com.finance.finance_tracker.entity.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class UserSoftDeleteCascadeIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("finance_tracker_test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("удаление пользователя мягко каскадируется на его счета и транзакции (C3, фикс)")
    void deleteUser_softDeleteCascadesToAccountsAndTransactions() {
        User user = new User();
        user.setName("Иван");
        user.setSurname("Иванов");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setEmail("soft-delete-cascade-test@example.com");
        user.setPassword("encoded");
        user.setActive(true);

        Account account = new Account();
        account.setName("Счет");
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(Currency.RUB);
        account.setUser(user);
        user.getAccounts().add(account);

        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("100.00"));
        tx.setType(TransactionType.EXPENSE);
        tx.setAccount(account);
        account.getTransactions().add(tx);

        entityManager.persist(user);
        entityManager.flush();
        Long userId = user.getId();
        Long accountId = account.getId();
        Long txId = tx.getId();
        entityManager.clear();

        User toDelete = userRepository.findById(userId).orElseThrow();
        userRepository.delete(toDelete);
        entityManager.flush();
        entityManager.clear();

        // 1. Обычный (отфильтрованный через @Where) путь — счёт больше не виден,
        //    как и должно быть после "удаления" с точки зрения пользователя
        assertThat(accountRepository.findById(accountId)).isEmpty();

        // 2. Но физически все три строки на месте — это и есть "мягкое" удаление,
        //    а не имитация его для одного User
        assertThat(nativeActive("users", userId)).isEqualTo(false);
        assertThat(nativeActive("accounts", accountId)).isEqualTo(false);
        assertThat(nativeActive("transactions", txId)).isEqualTo(false);
    }

    private Object nativeActive(String table, Long id) {
        return entityManager.getEntityManager()
                .createNativeQuery("SELECT active FROM finance_tracker." + table + " WHERE id = ?")
                .setParameter(1, id)
                .getSingleResult();
    }
}