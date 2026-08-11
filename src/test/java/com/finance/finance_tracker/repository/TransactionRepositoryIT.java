package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
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
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@Testcontainers
public class TransactionRepositoryIT {
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
    private TransactionRepository transactionRepository;
    @Autowired
    private TestEntityManager entityManager;

    private Long userId;
    private Long accountId;

    private void seedData() {
        User user = new User();
        user.setName("Иван");
        user.setSurname("Иванов");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setEmail("test@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        entityManager.persist(user);

        Account account = new Account();
        account.setName("Счет");
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(Currency.RUB);
        account.setUser(user);
        entityManager.persist(account);

        Category category = new Category();
        category.setName("Продукты");
        category.setUser(user);
        entityManager.persist(category);

        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("100.00"));
        tx.setType(TransactionType.EXPENSE);
        tx.setAccount(account);
        tx.setCategory(category);
        entityManager.persist(tx);

        entityManager.flush();
        entityManager.clear(); // критично: сбрасываем persistence context,
        // чтобы следующий запрос реально шёл в БД,
        // а не отдавал уже закэшированные в сессии объекты
        userId = user.getId();
        accountId = account.getId();
    }

    @Test
    @DisplayName("findByUserId подгружает account и category без отдельного запроса после detach")
    void findByUserId_fetchesAccountAndCategoryEagerly() {
        seedData();

        var result = transactionRepository.findByUserId(userId);
        entityManager.getEntityManager().clear(); // рвём связь с persistence context —
        // если account/category не были FETCH'нуты,
        // обращение к ним теперь бросит LazyInitializationException

        assertThat(result).hasSize(1);
        assertThatCode(() -> {
            String accountName = result.get(0).getAccount().getName();
            String categoryName = result.get(0).getCategory().getName();
            assertThat(accountName).isEqualTo("Счет");
            assertThat(categoryName).isEqualTo("Продукты");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("findRecentByUserId подгружает account без отдельного запроса после detach")
    void findRecentByUserId_fetchesAccountEagerly() {
        seedData();

        var result = transactionRepository.findRecentByUserId(userId, 10);
        entityManager.getEntityManager().clear();

        assertThat(result).hasSize(1);
        assertThatCode(() -> result.get(0).getAccount().getName())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("findRecentByUserId(int) — default-метод реально ограничивает выборку через PageRequest")
    void findRecentByUserId_intOverload_respectsLimit() {
        seedData();

        Account account = entityManager.getEntityManager().getReference(Account.class, accountId);
        Transaction secondTx = new Transaction();
        secondTx.setAmount(new BigDecimal("50.00"));
        secondTx.setType(TransactionType.INCOME);
        secondTx.setAccount(account);
        entityManager.persist(secondTx);
        entityManager.flush();
        entityManager.clear();

        var result = transactionRepository.findRecentByUserId(userId, 1);

        assertThat(result).hasSize(1); // а не 2, хотя в базе их теперь две
    }
}
