package com.finance.finance_tracker.exception;

public class EntityNotFoundException extends FinanceTrackerException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + " не найдена с id: " + id);
    }
}

