package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import com.finance.finance_tracker.mapper.AuditMapper;
import com.finance.finance_tracker.repository.AuditRepository;
import com.finance.finance_tracker.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditRepository auditRepository;
    private final AuditMapper auditMapper;


    @Async
    @Transactional
    public void log(Long userId, String username, String action, String entityType, Long entityId, String details) {
        Audit audit = new Audit();
        audit.setUserId(userId);
        audit.setUsername(username);
        audit.setAction(action);
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setDetails(details);
        auditRepository.save(audit);
    }

    @Transactional
    public List<AuditDto> getAllAudits() {
        List<Audit> list = auditRepository.findAll();
        return list.stream()
                .map(auditMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditDto> getRecentLogs(int limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageRequest = PageRequest.of(0, limit, sort);
        return auditRepository.findAll(pageRequest)
                .stream()
                .map(auditMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AuditDto> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditRepository.findAll(pageable)
                .map(auditMapper::toDto);
    }
}
