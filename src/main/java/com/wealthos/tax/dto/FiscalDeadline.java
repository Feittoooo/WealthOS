package com.wealthos.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A filing date. Texts travel as message keys so the frontend can show them in either language.
 * estimate is null when there's nothing to estimate (no entries yet, or an informative return).
 */
public record FiscalDeadline(
        LocalDate date,
        String titleKey,
        String descriptionKey,
        String periodKey,
        List<Object> periodArgs,
        BigDecimal estimate,
        long daysLeft) {
}
