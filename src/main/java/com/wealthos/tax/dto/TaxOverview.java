package com.wealthos.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Everything the tax page needs for one quarter.
public record TaxOverview(
        int year,
        int quarter,
        int currentYear,
        int currentQuarter,
        LocalDate today,
        QuarterSummary summary,
        List<QuarterSummary> yearSummaries,
        BigDecimal yearTotalToPay,
        List<TaxEntryView> entries,
        List<FiscalDeadline> deadlines,
        TaxSettingsView settings,
        List<CuotaMonth> cuotaMonths,
        BigDecimal cuotaPaidInYear) {
}
