package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.dto.UserDto;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.mapper.UserMapper;
import com.finance.finance_tracker.mapper.UserMapperImpl;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Доказывает S6: до @Mapping(target = "id", ignore = true) на
 * UserMapper.toEntity, подделанный id в форме регистрации (/register —
 * permitAll() в SecurityConfig, без аутентификации) заставлял Hibernate
 * трактовать новую сущность как существующую и вместо persist() выполнять
 * merge() поверх чужой строки.
 */
@DataJpaTest
@Testcontainers
class UserRegistrationIdSpoofingIT {

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
    private TestEntityManager entityManager;

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    @DisplayName("save() с подделанным чужим id не должен трогать существующего пользователя")
    void save_spoofedIdMatchingExistingUser_doesNotOverwriteExistingUser() {
        User existing = new User();
        existing.setName("Иван");
        existing.setSurname("Иванов");
        existing.setBirthday(LocalDate.of(1990, 1, 1));
        existing.setEmail("real-ivan@example.com");
        existing.setPassword("originalHash");
        existing.setActive(true);
        entityManager.persistAndFlush(existing);
        Long existingId = existing.getId();
        entityManager.clear();

        UserDto spoofedDto = new UserDto();
        spoofedDto.setId(existingId); // лишний параметр в POST /register
        spoofedDto.setName("Attacker");
        spoofedDto.setSurname("Attacker");
        spoofedDto.setBirthday(LocalDate.of(2000, 1, 1));
        spoofedDto.setEmail("attacker@example.com");
        spoofedDto.setPassword("irrelevant");

        userRepository.saveAndFlush(userMapper.toEntity(spoofedDto));
        entityManager.clear();

        User stillOriginal = userRepository.findById(existingId).orElseThrow();
        assertThat(stillOriginal.getEmail()).isEqualTo("real-ivan@example.com");
        assertThat(stillOriginal.getPassword()).isEqualTo("originalHash");
    }
}
