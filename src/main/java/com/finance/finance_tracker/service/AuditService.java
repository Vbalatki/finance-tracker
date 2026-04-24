package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.entity.Audit;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AuditService {
    void log(Long userId, String username, String action, String entityType, Long entityId, String details);
    List<AuditDto> getAllAudits();
    List<AuditDto> getRecentLogs(int limit);
    Page<AuditDto> getAuditLogs(int page, int size);
}
