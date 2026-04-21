package com.finance.finance_tracker.service;

public interface AuditService {
    void log(Long userId, String username, String action, String entityType, Long entityId, String details);
}
