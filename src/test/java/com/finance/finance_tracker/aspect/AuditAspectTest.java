package com.finance.finance_tracker.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finance_tracker.entity.SecurityUser;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link AuditAspect}.
 *
 * <p>Тест не поднимает реальный AOP-прокси (это потребовало бы полного
 * Spring-контекста) — вместо этого {@code audit(ProceedingJoinPoint)}
 * вызывается напрямую с моком {@link ProceedingJoinPoint}, что эквивалентно
 * с точки зрения проверки логики, но быстрее и изолированнее.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    // Реальный ObjectMapper — сериализация args/result в details не мокается,
    // это единственный способ честно проверить итоговую строку деталей.
    private AuditAspect aspect;

    // Классы-пустышки — нужны только для их simple name, по которому
    // determineEntityType() распознаёт тип сущности.
    static class AccountServiceImpl {}
    static class BudgetServiceImpl {}
    static class UnknownServiceImpl {}

    static class CreatedEntityWithId {
        private final Long id;
        CreatedEntityWithId(Long id) { this.id = id; }
    }

    static class CreatedEntityWithoutId {}

    @BeforeEach
    void setUp() {
        aspect = new AuditAspect(auditService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setActive(true);
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("action по имени метода")
    class ActionDetection {

        @Test
        @DisplayName("save*/registerUser -> CREATE, entityId берётся из поля id результата")
        void createAction_extractsIdFromResult() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveAccount");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(new CreatedEntityWithId(42L));

            aspect.audit(joinPoint);

            ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditService).log(isNull(), eq("anonymous"), eq("CREATE"), eq("Account"), eq(42L), detailsCaptor.capture());
            assertThat(detailsCaptor.getValue()).contains("Action: CREATE", "Entity: Account", "ID: 42");
        }

        @Test
        @DisplayName("update*/deposit/withdraw -> UPDATE, entityId берётся из первого Long-аргумента")
        void updateAction_extractsIdFromFirstArg() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("updateAccount");
            when(joinPoint.getArgs()).thenReturn(new Object[]{10L, "что-то"});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            verify(auditService).log(isNull(), eq("anonymous"), eq("UPDATE"), eq("Account"), eq(10L), any());
        }

        @Test
        @DisplayName("delete* -> DELETE, entityId берётся из первого Long-аргумента")
        void deleteAction_extractsIdFromFirstArg() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("deleteAccount");
            when(joinPoint.getArgs()).thenReturn(new Object[]{7L});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            verify(auditService).log(isNull(), eq("anonymous"), eq("DELETE"), eq("Account"), eq(7L), any());
        }

        @Test
        @DisplayName("неизвестное имя метода -> UNKNOWN")
        void unknownMethodName_returnsUnknownAction() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("recalculate");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            verify(auditService).log(isNull(), eq("anonymous"), eq("UNKNOWN"), any(), isNull(), any());
        }
    }

    @Nested
    @DisplayName("entityType по классу target'а")
    class EntityTypeDetection {

        @Test
        @DisplayName("класс без Account/Transaction/Category/Budget/User в имени -> Unknown")
        void unrecognizedClassName_returnsUnknown() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveSomething");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new UnknownServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            verify(auditService).log(any(), any(), any(), eq("Unknown"), any(), any());
        }

        @Test
        @DisplayName("Budget в имени класса -> Budget")
        void budgetClassName_returnsBudget() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveBudget");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new BudgetServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            verify(auditService).log(any(), any(), any(), eq("Budget"), any(), any());
        }
    }

    @Nested
    @DisplayName("извлечение id и детали")
    class DetailsAndEntityId {

        @Test
        @DisplayName("CREATE без поля id в результате — entityId остаётся null, ID не попадает в детали")
        void createWithoutIdField_entityIdNull() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveAccount");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(new CreatedEntityWithoutId());

            aspect.audit(joinPoint);

            ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditService).log(any(), any(), any(), any(), isNull(), detailsCaptor.capture());
            assertThat(detailsCaptor.getValue()).doesNotContain("ID:");
        }

        @Test
        @DisplayName("userId/username берутся из SecurityContext, если пользователь аутентифицирован")
        void authenticatedUser_passesRealUserIdAndUsername() throws Throwable {
            authenticateAs(5L, "ivan@example.com");
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveAccount");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            verify(auditService).log(eq(5L), eq("ivan@example.com"), any(), any(), any(), any());
        }

        @Test
        @DisplayName("длительность выполнения попадает в детали")
        void details_includeDuration() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveAccount");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditService).log(any(), any(), any(), any(), any(), detailsCaptor.capture());
            assertThat(detailsCaptor.getValue()).contains("Duration:").contains("ms");
        }
    }

    @Nested
    @DisplayName("исключения")
    class ExceptionHandling {

        @Test
        @DisplayName("исключение из proceed() пробрасывается наружу, но аудит всё равно записывается")
        void exceptionFromProceed_stillLogsAudit_andRethrows() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveAccount");
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            RuntimeException boom = new RuntimeException("Счёт не найден");
            when(joinPoint.proceed()).thenThrow(boom);

            assertThrows(RuntimeException.class, () -> aspect.audit(joinPoint));

            ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditService).log(any(), any(), any(), any(), any(), detailsCaptor.capture());
            assertThat(detailsCaptor.getValue()).contains("Exception: Счёт не найден");
        }

        @Test
        @DisplayName("если сериализация args в JSON падает — используется fallback на Arrays.toString, а не падает весь аудит")
        void jsonSerializationFails_fallsBackToArraysToString() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("saveAccount");

            // Самоссылающаяся структура — Jackson бросит JsonMappingException
            // на бесконечной рекурсии при попытке сериализовать её в JSON
            java.util.Map<String, Object> selfReferencing = new java.util.HashMap<>();
            selfReferencing.put("self", selfReferencing);
            when(joinPoint.getArgs()).thenReturn(new Object[]{selfReferencing});
            when(joinPoint.getTarget()).thenReturn(new AccountServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditService).log(any(), any(), any(), any(), any(), detailsCaptor.capture());
            // fallback-ветка использует Arrays.toString(args), а не ObjectMapper —
            // само наличие непустой строки "Args:" и то, что метод вообще
            // не бросил исключение наружу, подтверждает, что catch отработал
            assertThat(detailsCaptor.getValue()).contains("Args:");
        }
    }

    @Nested
    @DisplayName("resetSpending — отдельная явная ветка UPDATE")
    class ResetSpendingAction {

        @Test
        @DisplayName("resetSpending не начинается с update, но всё равно распознаётся как UPDATE")
        void resetSpending_recognizedAsUpdate() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("resetSpending");
            when(joinPoint.getArgs()).thenReturn(new Object[]{3L});
            when(joinPoint.getTarget()).thenReturn(new BudgetServiceImpl());
            when(joinPoint.proceed()).thenReturn(null);

            aspect.audit(joinPoint);

            verify(auditService).log(any(), any(), eq("UPDATE"), eq("Budget"), eq(3L), any());
        }
    }
}