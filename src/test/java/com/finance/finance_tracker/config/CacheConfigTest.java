package com.finance.finance_tracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    @DisplayName("cacheManager регистрирует оба кэша, которые реально используются в CurrencyApiServiceImpl")
    void cacheManager_registersBothCacheNames() {
        CacheManager cacheManager = new CacheConfig().cacheManager();

        assertThat(cacheManager.getCache("exchangeRates")).isNotNull();
        assertThat(cacheManager.getCache("fallbackExchangeRates")).isNotNull();
    }
}