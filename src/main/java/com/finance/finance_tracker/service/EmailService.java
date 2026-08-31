package com.finance.finance_tracker.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}