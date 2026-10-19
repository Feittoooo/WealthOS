package com.wealthos.tax.service;

import com.wealthos.tax.dto.FiscalDeadline;
import com.wealthos.tax.dto.QuarterSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Upcoming filing dates: 303 + 130 every quarter, the 390 in January and the Renta in June. */
@Service
@Transactional(readOnly = true)
public class FiscalCalendar {

    private final TaxService tax;
    private final Clock clock;

    public FiscalCalendar(TaxService tax, Clock clock) {
        this.tax = tax;
        this.clock = clock;
    }

    // Quarterly deadlines carry the 303 + 130 estimate when there are entries for that quarter.
    public List<FiscalDeadline> upcoming(int limit, boolean withEstimates) {
        LocalDate today = LocalDate.now(clock);
        Map<Integer, List<QuarterSummary>> cache = new HashMap<>();
        List<FiscalDeadline> all = new ArrayList<>();

        for (int year = today.getYear() - 1; year <= today.getYear() + 1; year++) {
            for (int q = 1; q <= 4; q++) {
                LocalDate date = TaxCalculator.filingDeadline(year, q);
                if (date.isBefore(today)) continue;
                BigDecimal estimate = null;
                if (withEstimates) {
                    QuarterSummary summary = cache.computeIfAbsent(year, tax::yearSummaries).get(q - 1);
                    if (summary.hasData()) estimate = summary.totalToPay();
                }
                all.add(new FiscalDeadline(date, "deadline.quarterlyTitle", "deadline.quarterly",
                        "common.quarter", List.of(q, String.valueOf(year)), estimate, days(today, date)));
            }
            LocalDate annualIva = TaxCalculator.nextBusinessDay(LocalDate.of(year + 1, 1, 30));
            if (!annualIva.isBefore(today)) {
                all.add(new FiscalDeadline(annualIva, "deadline.390Title", "deadline.390",
                        "common.year", List.of(String.valueOf(year)), null, days(today, annualIva)));
            }
            LocalDate renta = TaxCalculator.nextBusinessDay(LocalDate.of(year, 6, 30));
            if (!renta.isBefore(today)) {
                all.add(new FiscalDeadline(renta, "deadline.rentaTitle", "deadline.renta",
                        "common.year", List.of(String.valueOf(year - 1)), null, days(today, renta)));
            }
        }
        all.sort(Comparator.comparing(FiscalDeadline::date));
        return all.stream().limit(limit).toList();
    }

    private static long days(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to);
    }
}
