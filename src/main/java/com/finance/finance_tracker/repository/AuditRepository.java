package com.finance.finance_tracker.repository;

import com.finance.finance_tracker.entity.Audit;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository {
    void save(Audit audit);
}
