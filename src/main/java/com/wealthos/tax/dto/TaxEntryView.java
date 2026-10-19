package com.wealthos.tax.dto;

import com.wealthos.tax.domain.TaxEntry;
import com.wealthos.tax.domain.TaxEntryType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxEntryView(
        Long id,
        LocalDate date,
        String concept,
        String counterparty,
        String category,
        TaxEntryType type,
        BigDecimal baseAmount,
        BigDecimal ivaRate,
        BigDecimal ivaAmount,
        BigDecimal irpfRetentionRate,
        BigDecimal retentionAmount,
        boolean deductible,
        BigDecimal total) {

    public static TaxEntryView of(TaxEntry e) {
        return new TaxEntryView(e.getId(), e.getDate(), e.getConcept(), e.getCounterparty(), e.getCategory(),
                e.getType(), e.getBaseAmount(), e.getIvaRate(), e.getIvaAmount(), e.getIrpfRetentionRate(),
                e.getRetentionAmount(), e.isDeductible(), e.getTotal());
    }
}
