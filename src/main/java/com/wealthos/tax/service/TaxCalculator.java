package com.wealthos.tax.service;

import com.wealthos.tax.dto.QuarterSummary;
import com.wealthos.tax.domain.TaxEntry;
import com.wealthos.tax.domain.TaxSettings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Modelo 303 and 130 estimates. No database access here on purpose, so it's easy to test.
 *
 * Left out for now: prorrata, recargo de equivalencia, intra-EU operations, gastos de difícil
 * justificación, the housing loan deduction and carrying negative 303 results forward.
 */
public final class TaxCalculator {

    // art. 110 LIRPF
    public static final BigDecimal PAGO_FRACCIONADO_RATE = new BigDecimal("0.20");

    private TaxCalculator() {
    }

    // yearEntries = every entry dated in that year. The 130 needs the earlier quarters too.
    public static QuarterSummary compute(int year, int quarter, List<TaxEntry> yearEntries, TaxSettings settings) {
        if (quarter < 1 || quarter > 4) throw new IllegalArgumentException("Quarter must be 1-4");

        Set<YearMonth> paidMonths = settings.paidMonths();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        BigDecimal paidInPreviousQuarters = BigDecimal.ZERO;

        for (int q = 1; ; q++) {
            LocalDate from = quarterStart(year, q);
            LocalDate to = quarterEnd(year, q);

            List<TaxEntry> inQuarter = between(yearEntries, from, to);
            List<TaxEntry> ytd = between(yearEntries, yearStart, to);

            BigDecimal ytdIncome = sum(ytd, TaxEntry::isIncome, TaxEntry::getBaseAmount);
            BigDecimal ytdDeductible = sum(ytd, TaxCalculator::isDeductibleExpense, TaxEntry::getBaseAmount)
                    .add(deductibleCuota(settings, paidMonths, yearStart, to));
            BigDecimal ytdNet = ytdIncome.subtract(ytdDeductible);
            BigDecimal ytdRetentions = sum(ytd, TaxEntry::isIncome, TaxEntry::getRetentionAmount);

            BigDecimal gross = ytdNet.max(BigDecimal.ZERO)
                    .multiply(PAGO_FRACCIONADO_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal result130 = gross.subtract(ytdRetentions).subtract(paidInPreviousQuarters);

            if (q == quarter) {
                BigDecimal income = sum(inQuarter, TaxEntry::isIncome, TaxEntry::getBaseAmount);
                BigDecimal expenses = sum(inQuarter, e -> !e.isIncome(), TaxEntry::getBaseAmount);
                BigDecimal deductible = sum(inQuarter, TaxCalculator::isDeductibleExpense, TaxEntry::getBaseAmount);
                BigDecimal cuota = paidCuota(settings, paidMonths, from, to);
                BigDecimal cuotaDeduction = settings.isCuotaDeductible() ? cuota : BigDecimal.ZERO;
                BigDecimal ivaOut = sum(inQuarter, TaxEntry::isIncome, TaxEntry::getIvaAmount);
                BigDecimal ivaIn = sum(inQuarter, TaxCalculator::isDeductibleExpense, TaxEntry::getIvaAmount);
                BigDecimal retentions = sum(inQuarter, TaxEntry::isIncome, TaxEntry::getRetentionAmount);

                BigDecimal withheldIncome = sum(ytd,
                        e -> e.isIncome() && e.getIrpfRetentionRate().signum() > 0, TaxEntry::getBaseAmount);
                BigDecimal withheldPct = ytdIncome.signum() == 0 ? BigDecimal.ZERO
                        : withheldIncome.multiply(BigDecimal.valueOf(100)).divide(ytdIncome, 2, RoundingMode.HALF_UP);

                return new QuarterSummary(year, quarter, from, to, filingDeadline(year, quarter), inQuarter.size(),
                        income, expenses, deductible, cuota,
                        income.subtract(deductible).subtract(cuotaDeduction),
                        ivaOut, ivaIn, ivaOut.subtract(ivaIn), retentions,
                        ytdIncome, ytdDeductible, ytdNet, ytdRetentions, withheldPct,
                        gross, paidInPreviousQuarters, result130);
            }
            paidInPreviousQuarters = paidInPreviousQuarters.add(result130.max(BigDecimal.ZERO));
        }
    }

    public static LocalDate quarterStart(int year, int quarter) {
        return LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
    }

    public static LocalDate quarterEnd(int year, int quarter) {
        return quarterStart(year, quarter).plusMonths(3).minusDays(1);
    }

    public static int quarterOf(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    // 20 Apr, 20 Jul, 20 Oct and 30 Jan. Weekends move to Monday; regional holidays are ignored.
    public static LocalDate filingDeadline(int year, int quarter) {
        LocalDate date = switch (quarter) {
            case 1 -> LocalDate.of(year, 4, 20);
            case 2 -> LocalDate.of(year, 7, 20);
            case 3 -> LocalDate.of(year, 10, 20);
            case 4 -> LocalDate.of(year + 1, 1, 30);
            default -> throw new IllegalArgumentException("Quarter must be 1-4");
        };
        return nextBusinessDay(date);
    }

    public static LocalDate nextBusinessDay(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) return date.plusDays(2);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) return date.plusDays(1);
        return date;
    }

    private static boolean isDeductibleExpense(TaxEntry e) {
        return !e.isIncome() && e.isDeductible();
    }

    private static BigDecimal paidCuota(TaxSettings s, Set<YearMonth> paid, LocalDate from, LocalDate to) {
        long months = paid.stream()
                .filter(m -> !m.atDay(1).isBefore(from.withDayOfMonth(1)) && !m.atDay(1).isAfter(to))
                .count();
        return s.getMonthlyCuota().multiply(BigDecimal.valueOf(months));
    }

    private static BigDecimal deductibleCuota(TaxSettings s, Set<YearMonth> paid, LocalDate from, LocalDate to) {
        return s.isCuotaDeductible() ? paidCuota(s, paid, from, to) : BigDecimal.ZERO;
    }

    private static List<TaxEntry> between(List<TaxEntry> entries, LocalDate from, LocalDate to) {
        return entries.stream()
                .filter(e -> !e.getDate().isBefore(from) && !e.getDate().isAfter(to))
                .toList();
    }

    private static BigDecimal sum(List<TaxEntry> entries, Predicate<TaxEntry> filter,
                                  Function<TaxEntry, BigDecimal> value) {
        return entries.stream().filter(filter).map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
