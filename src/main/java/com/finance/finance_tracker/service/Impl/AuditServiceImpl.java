package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InvalidDataException;
import com.finance.finance_tracker.mapper.AuditMapper;
import com.finance.finance_tracker.repository.AuditRepository;
import com.finance.finance_tracker.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.Util.DataConstants.AUDIT_NOT_FOUND;
import static com.finance.finance_tracker.Util.DataConstants.DEFAULT_SORT_FIELD;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;
    private final AuditMapper auditMapper;

    @Async
    @Transactional
    public void log(Long userId, String username, String action, String entityType, Long entityId, String details) {
        if (userId == null) {
            log.warn("Попытка сохранить аудит без userId");
            return;
        }
        if (username == null || username.isBlank()) {
            log.warn("Попытка сохранить аудит без имени пользователя");
            return;
        }
        if (action == null || action.isBlank()) {
            log.warn("Попытка сохранить аудит без действия");
            return;
        }
        if (entityType == null || entityType.isBlank()) {
            log.warn("Попытка сохранить аудит без типа сущности");
            return;
        }

        Audit audit = new Audit();
        audit.setUserId(userId);
        audit.setUsername(username);
        audit.setAction(action);
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setDetails(details);

        auditRepository.save(audit);
        log.debug("Сохранена запись аудита: userId={}, action={}, entityType={}", userId, action, entityType);
    }

    @Transactional(readOnly = true)
    public List<AuditDto> getAllAudits() {
        log.debug("Запрос всех записей аудита");
        List<Audit> list = auditRepository.findAll();

        if (list.isEmpty()) {
            log.debug("Записи аудита не найдены");
        }

        return list.stream()
                .map(auditMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditDto> getRecentLogs(int limit) {
        if (limit <= 0) {
            throw new InvalidDataException("Лимит записей должен быть положительным числом, получено: " + limit);
        }

        log.debug("Запрос последних {} записей аудита", limit);

        Sort sort = Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD);
        PageRequest pageRequest = PageRequest.of(0, limit, sort);

        List<AuditDto> logs = auditRepository.findAll(pageRequest)
                .stream()
                .map(auditMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Найдено {} записей аудита", logs.size());

        return logs;
    }

    @Transactional(readOnly = true)
    public Page<AuditDto> getAuditLogs(int page, int size) {
        if (page < 0) {
            throw new InvalidDataException("Номер страницы не может быть отрицательным: " + page);
        }
        if (size <= 0) {
            throw new InvalidDataException("Размер страницы должен быть положительным: " + size);
        }

        log.debug("Запрос страницы аудита: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD));
        Page<AuditDto> auditPage = auditRepository.findAll(pageable)
                .map(auditMapper::toDto);

        log.debug("Найдено записей аудита: {}", auditPage.getTotalElements());

        return auditPage;
    }

    @Transactional(readOnly = true)
    public AuditDto getAuditById(Long id) {
        log.debug("Поиск записи аудита по id: {}", id);

        Audit audit = auditRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AUDIT_NOT_FOUND + ", id: " + id));

        return auditMapper.toDto(audit);
    }

    @Transactional
    public void deleteAudit(Long id) {
        log.debug("Удаление записи аудита по id: {}", id);

        if (!auditRepository.existsById(id)) {
            throw new EntityNotFoundException(AUDIT_NOT_FOUND + ", id: " + id);
        }

        auditRepository.deleteById(id);
        log.info("Удалена запись аудита с id: {}", id);
    }

    @Transactional
    public void deleteAllAudits() {
        log.warn("Удаление всех записей аудита");
        auditRepository.deleteAll();
        log.info("Все записи аудита удалены");
    }
}