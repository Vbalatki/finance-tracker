package com.finance.finance_tracker.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class ExchangeRateResponseDto {
    @JsonProperty("base_code")
    private String baseCode;
    @JsonProperty("conversion_rates")
    private Map<String, Double> conversionRates;
    @JsonProperty("time_last_update_utc")
    private String timeLastUpdateUtc;
}
