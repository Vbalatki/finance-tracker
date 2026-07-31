package com.finance.finance_tracker.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TBankStatementResponseDto {

    @JsonProperty("operations")
    private List<TBankOperationDto> operations;

    @JsonProperty("balances")
    private TBankBalancesDto balances;

    @JsonProperty("nextCursor")
    private String nextCursor;

    public List<TBankOperationDto> getOperations() { return operations; }
    public void setOperations(List<TBankOperationDto> operations) { this.operations = operations; }
    public TBankBalancesDto getBalances() { return balances; }
    public void setBalances(TBankBalancesDto balances) { this.balances = balances; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
}