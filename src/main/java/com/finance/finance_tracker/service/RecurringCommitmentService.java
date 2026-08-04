package com.finance.finance_tracker.service;

import com.finance.finance_tracker.dto.RecurringCommitmentDto;

import java.util.List;

public interface RecurringCommitmentService {

    RecurringCommitmentDto save(RecurringCommitmentDto dto, Long userId);

    List<RecurringCommitmentDto> getByUserId(Long userId);

    void toggleActive(Long id, Long currentUserId);

    void delete(Long id, Long currentUserId);
}