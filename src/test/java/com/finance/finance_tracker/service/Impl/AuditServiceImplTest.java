package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.AuditMapper;
import com.finance.finance_tracker.repository.AuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link AuditServiceImpl}.
 * Метод log() помечен @Async, но в юнит-тестах (без Spring-контекста)
 * вызывается синхронно — это нормально для проверки бизнес-логики валидации.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditRepository auditRepository;
    @Mock
    private AuditMapper auditMapper;

    @InjectMocks
    private AuditServiceImpl auditService;

    private Audit audit;
    private AuditDto auditDto;

    @BeforeEach
    void setUp() {
        audit = new Audit();
        audit.setId(1L);
        audit.setUserId(1L);
        audit.setUsername("ivan@example.com");
        audit.setAction("CREATE");
        audit.setEntityType("Account");

        auditDto = new AuditDto();
        auditDto.setId(1L);
    }

    @Nested
    @DisplayName("log")
    class Log {

        @Test
        @DisplayName("сохраняет запись аудита, когда все обязательные поля заполнены")
        void log_validData_saves() {
            auditService.log(1L, "ivan@example.com", "CREATE", "Account", 10L, "details");

            verify(auditRepository).save(any(Audit.class));
        }

        @Test
        @DisplayName("не сохраняет запись, если userId равен null")
        void log_nullUserId_skipsSave() {
            auditService.log(null, "ivan@example.com", "CREATE", "Account", 10L, "details");

            verify(auditRepository, never()).save(any());
        }

        @Test
        @DisplayName("не сохраняет запись, если username пуст")
        void log_blankUsername_skipsSave() {
            auditService.log(1L, "  ", "CREATE", "Account", 10L, "details");

            verify(auditRepository, never()).save(any());
        }

        @Test
        @DisplayName("не сохраняет запись, если action пуст")
        void log_blankAction_skipsSave() {
            auditService.log(1L, "ivan@example.com", "", "Account", 10L, "details");

            verify(auditRepository, never()).save(any());
        }

        @Test
        @DisplayName("не сохраняет запись, если entityType пуст")
        void log_blankEntityType_skipsSave() {
            auditService.log(1L, "ivan@example.com", "CREATE", null, 10L, "details");

            verify(auditRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("выборки")
    class Queries {

        @Test
        @DisplayName("getAllAudits возвращает все записи")
        void getAllAudits_success() {
            when(auditRepository.findAll()).thenReturn(List.of(audit));
            when(auditMapper.toDto(audit)).thenReturn(auditDto);

            assertThat(auditService.getAllAudits()).containsExactly(auditDto);
        }

        @Test
        @DisplayName("getRecentLogs возвращает последние N записей")
        void getRecentLogs_success() {
            Page<Audit> page = new PageImpl<>(List.of(audit));
            when(auditRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(auditMapper.toDto(audit)).thenReturn(auditDto);

            assertThat(auditService.getRecentLogs(10)).containsExactly(auditDto);
        }

        @Test
        @DisplayName("getRecentLogs бросает InvalidDataException для лимита <= 0")
        void getRecentLogs_invalidLimit_throws() {
            assertThrows(InvalidDataException.class, () -> auditService.getRecentLogs(0));
            assertThrows(InvalidDataException.class, () -> auditService.getRecentLogs(-1));
        }

        @Test
        @DisplayName("getAuditLogs возвращает страницу записей")
        void getAuditLogs_success() {
            Page<Audit> page = new PageImpl<>(List.of(audit));
            when(auditRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(auditMapper.toDto(audit)).thenReturn(auditDto);

            Page<AuditDto> result = auditService.getAuditLogs(0, 50);

            assertThat(result.getContent()).containsExactly(auditDto);
        }

        @Test
        @DisplayName("getAuditLogs бросает InvalidDataException для отрицательной страницы")
        void getAuditLogs_negativePage_throws() {
            assertThrows(InvalidDataException.class, () -> auditService.getAuditLogs(-1, 50));
        }

        @Test
        @DisplayName("getAuditLogs бросает InvalidDataException для неположительного размера страницы")
        void getAuditLogs_invalidSize_throws() {
            assertThrows(InvalidDataException.class, () -> auditService.getAuditLogs(0, 0));
        }
    }

    @Nested
    @DisplayName("getAuditById / удаление")
    class GetAndDelete {

        @Test
        @DisplayName("getAuditById возвращает запись по id")
        void getAuditById_success() {
            when(auditRepository.findById(1L)).thenReturn(Optional.of(audit));
            when(auditMapper.toDto(audit)).thenReturn(auditDto);

            assertThat(auditService.getAuditById(1L)).isEqualTo(auditDto);
        }

        @Test
        @DisplayName("getAuditById бросает EntityNotFoundException, если запись не найдена")
        void getAuditById_notFound_throws() {
            when(auditRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> auditService.getAuditById(404L));
        }

        @Test
        @DisplayName("deleteAudit удаляет существующую запись")
        void deleteAudit_success() {
            when(auditRepository.existsById(1L)).thenReturn(true);

            auditService.deleteAudit(1L);

            verify(auditRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deleteAudit бросает EntityNotFoundException для несуществующей записи")
        void deleteAudit_notFound_throws() {
            when(auditRepository.existsById(404L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> auditService.deleteAudit(404L));
        }

        @Test
        @DisplayName("deleteAllAudits удаляет все записи")
        void deleteAllAudits_success() {
            auditService.deleteAllAudits();

            verify(auditRepository).deleteAll();
        }
    }
}
