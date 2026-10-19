package com.wealthos.tax.domain;

public enum TaxEntryType {
    INGRESO,
    GASTO;

    public String getMessageKey() {
        return "entryType." + name();
    }
}
