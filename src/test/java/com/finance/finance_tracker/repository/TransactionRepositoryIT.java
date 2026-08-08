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
    @DisplayName("sumAmountByUserIdAndType суммирует транзакции со ВСЕХ счетов пользователя")
    void sumAmountByUserIdAndType_sumsAcrossAllUserAccounts() {
        User user = new User();
        user.setName("Пётр");
        user.setSurname("Петров");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setEmail("multi-account@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        entityManager.persist(user);

        Account acc1 = new Account();
        acc1.setName("Счет 1");
        acc1.setBalance(BigDecimal.ZERO);
        acc1.setCurrency(Currency.RUB);
        acc1.setUser(user);
        entityManager.persist(acc1);

        Account acc2 = new Account();
        acc2.setName("Счет 2");
        acc2.setBalance(BigDecimal.ZERO);
        acc2.setCurrency(Currency.RUB);
        acc2.setUser(user);
        entityManager.persist(acc2);

        Transaction tx1 = new Transaction();
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setType(TransactionType.INCOME);
        tx1.setAccount(acc1);
        entityManager.persist(tx1);

        Transaction tx2 = new Transaction();
        tx2.setAmount(new BigDecimal("50.00"));
        tx2.setType(TransactionType.INCOME);
        tx2.setAccount(acc2);
        entityManager.persist(tx2);

        entityManager.flush();
        entityManager.clear();

        BigDecimal total = transactionRepository
                .sumAmountByUserIdAndType(user.getId(), TransactionType.INCOME)
                .orElse(BigDecimal.ZERO);

        assertThat(total).isEqualByComparingTo("150.00"); // с обоих счетов, не с одного
    }
}
