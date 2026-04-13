package com.finance.finance_tracker.entity.enums;

public enum TransactionType {
    INCOME("Поступление"),
    EXPENSE("Траты");

    private String value;
    private TransactionType(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
