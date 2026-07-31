package com.finance.finance_tracker.exception;

public class BankIntegrationException extends FinanceTrackerException {
    public BankIntegrationException(String message) {
        super(message);
    }

    public BankIntegrationException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}