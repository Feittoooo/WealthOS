package com.wealthos.tax.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

// Built by TaxCalculator. The ytd* fields feed the 130, which is cumulative from 1 January.
public record QuarterSummary(
        int year,
        int quarter,
        LocalDate from,
        LocalDate to,
        LocalDate filingDeadline,
        int entryCount,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal deductibleExpenses,
        BigDecimal cuota,
        BigDecimal netIncome,
        BigDecimal ivaRepercutido,
        BigDecimal ivaSoportado,
        BigDecimal ivaResult,
        BigDecimal retentions,
        BigDecimal ytdIncome,
        BigDecimal ytdDeductible,
        BigDecimal ytdNetIncome,
        BigDecimal ytdRetentions,
        BigDecimal ytdIncomeWithRetentionPct,
        BigDecimal modelo130Gross,
        BigDecimal previousPayments,
        BigDecimal modelo130Result) {

    @JsonProperty
    public String label() {
        return "Q" + quarter + " " + year;
    }

    @JsonProperty
    public BigDecimal ivaToPay() {
        return ivaResult.max(BigDecimal.ZERO);
    }

    @JsonProperty
    public BigDecimal modelo130ToPay() {
        return modelo130Result.max(BigDecimal.ZERO);
    }

    @JsonProperty
    public BigDecimal totalToPay() {
        return ivaToPay().add(modelo130ToPay());
    }

    @JsonProperty
    public boolean hasData() {
        return entryCount > 0;
    }

    // Art. 109.1 RIRPF: no 130 if 70 % or more of your income had withholding.
    // The rule really looks at the previous year, so this is only a hint.
    @JsonProperty
    public boolean likely130Exempt() {
        return ytdIncome.signum() > 0 && ytdIncomeWithRetentionPct.compareTo(BigDecimal.valueOf(70)) >= 0;
    }
}
