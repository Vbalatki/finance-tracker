package com.finance.finance_tracker.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TBankBalancesDto {

    @JsonProperty("balanceEnd")
    private BigDecimal balanceEnd;

    public BigDecimal getBalanceEnd() { return balanceEnd; }
    public void setBalanceEnd(BigDecimal balanceEnd) { this.balanceEnd = balanceEnd; }
}