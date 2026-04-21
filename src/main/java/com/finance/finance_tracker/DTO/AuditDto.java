package com.finance.finance_tracker.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditDto {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}
