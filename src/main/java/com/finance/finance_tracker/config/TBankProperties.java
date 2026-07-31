package com.finance.finance_tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tbank")
public record TBankProperties(String url, String token) {
}