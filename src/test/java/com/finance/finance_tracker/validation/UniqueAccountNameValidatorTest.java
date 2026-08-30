package com.finance.finance_tracker.validation;

import com.finance.finance_tracker.dto.AccountDto;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.AccountRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueAccountNameValidatorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    private UniqueAccountNameValidator validator;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPassword("encoded");
        user.setActive(true);
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("пустое имя валидно — это забота @NotBlank")
    void isValid_blankName_returnsTrue() {
        validator = new UniqueAccountNameValidator(accountRepository);
        AccountDto dto = new AccountDto();
        dto.setName("  ");

        assertThat(validator.isValid(dto, context)).isTrue();
        verifyNoInteractions(accountRepository);
    }

    @Test
    @DisplayName("без аутентификации — валидно (проверять просто не с кем сравнивать)")
    void isValid_noAuthentication_returnsTrue() {
        SecurityContextHolder.clearContext();

        validator = new UniqueAccountNameValidator(accountRepository);
        AccountDto dto = new AccountDto();
        dto.setName("Основной счет");

        assertThat(validator.isValid(dto, context)).isTrue();
        verifyNoInteractions(accountRepository);
    }

    @Test
    @DisplayName("создание нового счёта со свободным именем — валидно")
    void isValid_creatingWithFreeName_returnsTrue() {
        authenticateAs(1L);
        when(accountRepository.existsByNameAndUserId("Новый счет", 1L)).thenReturn(false);

        validator = new UniqueAccountNameValidator(accountRepository);
        AccountDto dto = new AccountDto();
        dto.setName("Новый счет");

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("создание нового счёта с занятым именем — невалидно")
    void isValid_creatingWithDuplicateName_returnsFalse() {
        authenticateAs(1L);
        when(accountRepository.existsByNameAndUserId("Основной счет", 1L)).thenReturn(true);

        validator = new UniqueAccountNameValidator(accountRepository);
        AccountDto dto = new AccountDto();
        dto.setName("Основной счет");

        assertThat(validator.isValid(dto, context)).isFalse();
    }

    @Test
    @DisplayName("при редактировании исключает саму себя из проверки")
    void isValid_editingOwnAccount_excludesSelf_returnsTrue() {
        authenticateAs(1L);
        when(accountRepository.existsByNameAndUserIdAndIdNot("Основной счет", 1L, 10L)).thenReturn(false);

        validator = new UniqueAccountNameValidator(accountRepository);
        AccountDto dto = new AccountDto();
        dto.setId(10L);
        dto.setName("Основной счет");

        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("не доверяет dto.userId — проверяет именно currentUserId из SecurityContext")
    void isValid_ignoresSpoofedDtoUserId_usesSecurityContextInstead() {
        authenticateAs(1L);
        when(accountRepository.existsByNameAndUserId("Счет", 1L)).thenReturn(false);

        validator = new UniqueAccountNameValidator(accountRepository);
        AccountDto dto = new AccountDto();
        dto.setName("Счет");
        dto.setUserId(999L); // подделанное значение с формы — не должно повлиять

        assertThat(validator.isValid(dto, context)).isTrue();
        // именно currentUserId (1L), а не dto.getUserId() (999L)
    }
}
