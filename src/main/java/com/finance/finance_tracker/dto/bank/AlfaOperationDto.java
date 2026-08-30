package com.finance.finance_tracker.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * НЕ подтверждено на 100% против реального Swagger statement/transactions —
 * собран по образцу подтверждённой структуры GET /operations/{id} из
 * открытой документации Alfa API. Перед продакшеном свериться с реальным
 * ответом песочницы (sandbox.alfabank.ru/swagger-ui/), как README уже
 * предупреждает для T-Bank.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfaOperationDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("dateTime")
    private String dateTime;

    @JsonProperty("title")
    private String title;

    @JsonProperty("amount")
    private AlfaAmountDto amount;

    @JsonProperty("direction")
    private String direction; // "INCOME" | "EXPENSE"

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AlfaAmountDto getAmount() { return amount; }
    public void setAmount(AlfaAmountDto amount) { this.amount = amount; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlfaAmountDto {
        @JsonProperty("value")
        private BigDecimal value;
        @JsonProperty("currency")
        private String currency;
        @JsonProperty("minorUnits")
        private Integer minorUnits;

        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public Integer getMinorUnits() { return minorUnits; }
        public void setMinorUnits(Integer minorUnits) { this.minorUnits = minorUnits; }
    }
}
