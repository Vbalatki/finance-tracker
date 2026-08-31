package com.finance.finance_tracker.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;

    private final Cache<String, AtomicInteger> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public void recordFailedAttempt(String key) {
        attemptsCache.asMap()
                .computeIfAbsent(normalize(key), k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public void clearAttempts(String key) {
        attemptsCache.invalidate(normalize(key));
    }

    public boolean isBlocked(String key) {
        AtomicInteger attempts = attemptsCache.getIfPresent(normalize(key));
        return attempts != null && attempts.get() >= MAX_ATTEMPTS;
    }

    private String normalize(String key) {
        return key == null ? "" : key.toLowerCase().trim();
    }
}