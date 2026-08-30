package com.finance.finance_tracker.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfaStatementResponseDto {

    @JsonProperty("operations")
    private List<AlfaOperationDto> operations;

    @JsonProperty("nextPage")
    private String nextPage;

    public List<AlfaOperationDto> getOperations() { return operations; }
    public void setOperations(List<AlfaOperationDto> operations) { this.operations = operations; }
    public String getNextPage() { return nextPage; }
    public void setNextPage(String nextPage) { this.nextPage = nextPage; }
}
