package com.finance.finance_tracker.controller;


import com.finance.finance_tracker.service.CurrencyApiService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyApiService currencyApiService;

    @GetMapping("/rates")
    public ResponseEntity<Map<String, Double>> getRates(@RequestParam(defaultValue = "USD") String base) {
        return ResponseEntity.ok(currencyApiService.getExchangeRates(base.toUpperCase()));
    }

    @GetMapping("/convert")
    public ResponseEntity<BigDecimal> convert(@RequestParam String from,
                                              @RequestParam String to,
                                              @RequestParam BigDecimal amount) {
        BigDecimal result = currencyApiService.convertCurrency(from, to, amount);
        return ResponseEntity.ok(result);
    }
}
