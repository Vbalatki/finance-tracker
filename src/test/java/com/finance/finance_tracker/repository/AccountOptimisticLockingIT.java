package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверяет, что @Version на Account реально ловит гонку между двумя
 * "параллельными" изменениями одного счёта — то есть ровно то, что
 * Mockito-тесты AccountServiceImplTest в принципе не могут проверить,
 * потому что там accountRepository.save() — это просто мок без
 * настоящего Hibernate optimistic-lock check.
 */
@DataJpaTest
@Testcontainers
class AccountOptimisticLockingIT {

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
    @DisplayName("второе сохранение с устаревшим version бросает ObjectOptimisticLockingFailureException")
    void concurrentUpdate_secondSaveWithStaleVersion_throws() {
        User user = new User();
        user.setName("Иван");
        user.setSurname("Иванов");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setEmail("optimistic-lock-test@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        entityManager.persist(user);

        Account account = new Account();
        account.setName("Счет");
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(Currency.RUB);
        account.setUser(user);
        entityManager.persist(account);
        entityManager.flush();
        Long accountId = account.getId();

        entityManager.clear(); // "закрываем сессию" — имитируем, что дальше
        // работаем с двумя независимыми копиями,
        // как это было бы в двух разных HTTP-запросах

        // "Первый запрос" читает счёт и пополняет баланс
        Account firstRead = accountRepository.findById(accountId).orElseThrow();
        entityManager.detach(firstRead);

        // "Второй запрос" читает тот же счёт ДО того, как первый сохранил
        Account secondRead = accountRepository.findById(accountId).orElseThrow();
        entityManager.detach(secondRead);

        // Первый запрос сохраняет — проходит успешно, version инкрементится в БД
        firstRead.setBalance(firstRead.getBalance().add(new BigDecimal("100.00")));
        accountRepository.saveAndFlush(firstRead);

        // Второй запрос пытается сохранить с уже устаревшим version —
        // должен получить конфликт, а не молча затереть чужое обновление
        secondRead.setBalance(secondRead.getBalance().subtract(new BigDecimal("50.00")));
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> accountRepository.saveAndFlush(secondRead));

        // Итоговый баланс в БД — это результат ПЕРВОГО (успешного) сохранения,
        // второе обновление не применилось незаметно
        entityManager.clear();
        Account finalState = accountRepository.findById(accountId).orElseThrow();
        assertThat(finalState.getBalance()).isEqualByComparingTo("1100.00");
    }
}