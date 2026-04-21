package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.entity.Audit;
import com.finance.finance_tracker.repository.AuditRepository;
import com.finance.finance_tracker.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditRepository auditRepository;

    @Async
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

}
