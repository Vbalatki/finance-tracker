package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
class AccountRepositoryIT {

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
    private AccountRepository accountRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("БД отклоняет второй счёт с тем же именем у того же пользователя (C1)")
    void duplicateAccountNameForSameUser_violatesUniqueConstraint() {
        User user = new User();
        user.setName("Иван");
        user.setSurname("Иванов");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setEmail("unique-account-name-test@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        entityManager.persist(user);

        Account first = new Account();
        first.setName("Основной счет");
        first.setBalance(BigDecimal.ZERO);
        first.setCurrency(Currency.RUB);
        first.setUser(user);
        entityManager.persistAndFlush(first);

        Account duplicate = new Account();
        duplicate.setName("Основной счет");
        duplicate.setBalance(BigDecimal.ZERO);
        duplicate.setCurrency(Currency.USD);
        duplicate.setUser(user);

        assertThrows(DataIntegrityViolationException.class,
                () -> accountRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("после мягкого удаления имя счёта освобождается для повторного использования")
    void softDeletedAccountName_becomesAvailableForReuse() {
        User user = new User();
        user.setName("Иван");
        user.setSurname("Иванов");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setEmail("reuse-name-test@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        entityManager.persist(user);

        Account original = new Account();
        original.setName("Основной счет");
        original.setBalance(BigDecimal.ZERO);
        original.setCurrency(Currency.RUB);
        original.setUser(user);
        entityManager.persistAndFlush(original);
        Long originalId = original.getId();

        accountRepository.delete(original);
        entityManager.flush();
        entityManager.clear();

        Account recreated = new Account();
        recreated.setName("Основной счет"); // то же имя, что у мягко удалённого
        recreated.setBalance(BigDecimal.ZERO);
        recreated.setCurrency(Currency.RUB);
        recreated.setUser(entityManager.getEntityManager().getReference(User.class, user.getId()));

        assertThatCode(() -> accountRepository.saveAndFlush(recreated))
                .doesNotThrowAnyException();

        Object rawActive = entityManager.getEntityManager()
                .createNativeQuery("SELECT active FROM finance_tracker.accounts WHERE id = ?")
                .setParameter(1, originalId)
                .getSingleResult();
        assertThat(rawActive).isEqualTo(false);
    }
}