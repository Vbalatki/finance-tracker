package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import com.finance.finance_tracker.mapper.AuditMapper;
import com.finance.finance_tracker.repository.AuditRepository;
import com.finance.finance_tracker.service.AuditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
}
