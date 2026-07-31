package com.finance.finance_tracker.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TBankOperationDto {

    @JsonProperty("operationId")
    private String operationId;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("typeOfOperation")
    private String typeOfOperation;

    @JsonProperty("category")
    private String category;

    @JsonProperty("description")
    private String description;

    @JsonProperty("payPurpose")
    private String payPurpose;

    @JsonProperty("accountAmount")
    private BigDecimal accountAmount;

    @JsonProperty("accountCurrencyDigitalCode")
    private String accountCurrencyDigitalCode;

    @JsonProperty("operationDate")
    private String operationDate;

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getTypeOfOperation() { return typeOfOperation; }
    public void setTypeOfOperation(String typeOfOperation) { this.typeOfOperation = typeOfOperation; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPayPurpose() { return payPurpose; }
    public void setPayPurpose(String payPurpose) { this.payPurpose = payPurpose; }
    public BigDecimal getAccountAmount() { return accountAmount; }
    public void setAccountAmount(BigDecimal accountAmount) { this.accountAmount = accountAmount; }
    public String getAccountCurrencyDigitalCode() { return accountCurrencyDigitalCode; }
    public void setAccountCurrencyDigitalCode(String accountCurrencyDigitalCode) { this.accountCurrencyDigitalCode = accountCurrencyDigitalCode; }
    public String getOperationDate() { return operationDate; }
    public void setOperationDate(String operationDate) { this.operationDate = operationDate; }
}