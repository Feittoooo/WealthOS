package com.wealthos.tax;

import com.wealthos.tax.dto.QuarterSummary;
import com.wealthos.tax.service.TaxCalculator;
import com.wealthos.tax.domain.TaxEntry;
import com.wealthos.tax.domain.TaxEntryType;
import com.wealthos.tax.domain.TaxSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TaxCalculatorTest {

    private static TaxEntry income(String date, String base, int irpf) {
        TaxEntry e = new TaxEntry();
        e.setDate(LocalDate.parse(date));
        e.setConcept("Invoice");
        e.setType(TaxEntryType.INGRESO);
        e.setBaseAmount(new BigDecimal(base));
        e.setIvaRate(BigDecimal.valueOf(21));
        e.setIrpfRetentionRate(BigDecimal.valueOf(irpf));
        e.setDeductible(false);
        return e;
    }

    private static TaxEntry expense(String date, String base, boolean deductible) {
        TaxEntry e = new TaxEntry();
        e.setDate(LocalDate.parse(date));
        e.setConcept("Expense");
        e.setType(TaxEntryType.GASTO);
        e.setBaseAmount(new BigDecimal(base));
        e.setIvaRate(BigDecimal.valueOf(21));
        e.setIrpfRetentionRate(BigDecimal.ZERO);
        e.setDeductible(deductible);
        return e;
    }

    private static TaxSettings settings(String cuota, boolean deductible, YearMonth... paid) {
        TaxSettings s = new TaxSettings();
        s.setMonthlyCuota(new BigDecimal(cuota));
        s.setCuotaDeductible(deductible);
        s.setPaidMonths(Set.of(paid));
        return s;
    }

    @Test
    void modelo303IsIvaChargedMinusDeductibleIvaPaid() {
        List<TaxEntry> entries = List.of(
                income("2026-02-10", "1000.00", 15),
                expense("2026-03-01", "200.00", true),
                expense("2026-03-02", "100.00", false)); // non-deductible IVA is ignored

        QuarterSummary q1 = TaxCalculator.compute(2026, 1, entries, settings("0", false));

        assertThat(q1.ivaRepercutido()).isEqualByComparingTo("210.00");
        assertThat(q1.ivaSoportado()).isEqualByComparingTo("42.00");
        assertThat(q1.ivaResult()).isEqualByComparingTo("168.00");
        assertThat(q1.expenses()).isEqualByComparingTo("300.00");
        assertThat(q1.deductibleExpenses()).isEqualByComparingTo("200.00");
    }

    @Test
    void modelo130Is20PercentOfNetIncomeMinusWithholdings() {
        // Q1: 4000 income without withholding, 1000 deductible expenses -> 20% of 3000 = 600
        List<TaxEntry> entries = List.of(
                income("2026-01-15", "4000.00", 0),
                expense("2026-02-15", "1000.00", true));

        QuarterSummary q1 = TaxCalculator.compute(2026, 1, entries, settings("0", false));

        assertThat(q1.ytdNetIncome()).isEqualByComparingTo("3000.00");
        assertThat(q1.modelo130Gross()).isEqualByComparingTo("600.00");
        assertThat(q1.modelo130ToPay()).isEqualByComparingTo("600.00");
    }

    @Test
    void modelo130IsCumulativeAndSubtractsPreviousPayments() {
        List<TaxEntry> entries = List.of(
                income("2026-01-15", "4000.00", 0),   // Q1 net 4000 -> pay 800
                income("2026-04-15", "1000.00", 0));  // Q2 cumulative 5000 -> 1000 - 800 = 200

        QuarterSummary q2 = TaxCalculator.compute(2026, 2, entries, settings("0", false));

        assertThat(q2.ytdIncome()).isEqualByComparingTo("5000.00");
        assertThat(q2.modelo130Gross()).isEqualByComparingTo("1000.00");
        assertThat(q2.previousPayments()).isEqualByComparingTo("800.00");
        assertThat(q2.modelo130ToPay()).isEqualByComparingTo("200.00");
    }

    @Test
    void withholdingsReduceModelo130AndCanMakeItZero() {
        // 1000 at 15% withholding: 20% of 1000 = 200, minus 150 withheld = 50
        QuarterSummary q1 = TaxCalculator.compute(2026, 1,
                List.of(income("2026-03-01", "1000.00", 15)), settings("0", false));
        assertThat(q1.retentions()).isEqualByComparingTo("150.00");
        assertThat(q1.modelo130ToPay()).isEqualByComparingTo("50.00");
        assertThat(q1.likely130Exempt()).isTrue();

        // A loss quarter never produces a negative payment
        QuarterSummary loss = TaxCalculator.compute(2026, 1,
                List.of(expense("2026-03-01", "500.00", true)), settings("0", false));
        assertThat(loss.modelo130ToPay()).isEqualByComparingTo("0");
        assertThat(loss.ivaToPay()).isEqualByComparingTo("0");
        assertThat(loss.ivaResult()).isEqualByComparingTo("-105.00");
    }

    @Test
    void paidCuotaIsDeductibleWhenEnabled() {
        List<TaxEntry> entries = List.of(income("2026-02-01", "3000.00", 0));
        TaxSettings s = settings("300.00", true, YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 4));

        QuarterSummary q1 = TaxCalculator.compute(2026, 1, entries, s);

        assertThat(q1.cuota()).isEqualByComparingTo("600.00");            // Jan + Feb (April is Q2)
        assertThat(q1.netIncome()).isEqualByComparingTo("2400.00");
        assertThat(q1.modelo130ToPay()).isEqualByComparingTo("480.00");

        QuarterSummary notDeducted = TaxCalculator.compute(2026, 1, entries, settings("300.00", false, YearMonth.of(2026, 1)));
        assertThat(notDeducted.netIncome()).isEqualByComparingTo("3000.00");
    }

    @Test
    void entriesOutsideTheYearAreIgnoredByQuarterBoundaries() {
        List<TaxEntry> entries = List.of(
                income("2026-03-31", "100.00", 0),
                income("2026-04-01", "200.00", 0));

        assertThat(TaxCalculator.compute(2026, 1, entries, settings("0", false)).income()).isEqualByComparingTo("100.00");
        assertThat(TaxCalculator.compute(2026, 2, entries, settings("0", false)).income()).isEqualByComparingTo("200.00");
    }

    @Test
    void deadlinesMoveFromWeekendToMonday() {
        // 20 April 2025 was a Sunday
        assertThat(TaxCalculator.filingDeadline(2025, 1)).isEqualTo(LocalDate.of(2025, 4, 21));
        // 20 October 2026 is a Tuesday
        assertThat(TaxCalculator.filingDeadline(2026, 3)).isEqualTo(LocalDate.of(2026, 10, 20));
        // Q4 is filed until 30 January of the following year (30 Jan 2027 is a Saturday)
        assertThat(TaxCalculator.filingDeadline(2026, 4)).isEqualTo(LocalDate.of(2027, 2, 1));
    }
}
