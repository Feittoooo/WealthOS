package com.wealthos.tax.service;

import com.wealthos.activity.ActivityLog;
import com.wealthos.activity.TransactionType;
import com.wealthos.common.error.NotFoundException;
import com.wealthos.tax.domain.TaxEntry;
import com.wealthos.tax.domain.TaxEntryType;
import com.wealthos.tax.domain.TaxSettings;
import com.wealthos.tax.dto.CuotaMonth;
import com.wealthos.tax.dto.QuarterSummary;
import com.wealthos.tax.dto.TaxEntryRequest;
import com.wealthos.tax.dto.TaxSettingsRequest;
import com.wealthos.tax.repository.TaxEntryRepository;
import com.wealthos.tax.repository.TaxSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TaxService {

    private final TaxEntryRepository entries;
    private final TaxSettingsRepository settingsRepository;
    private final ActivityLog activity;
    private final Clock clock;

    public TaxService(TaxEntryRepository entries, TaxSettingsRepository settingsRepository,
                      ActivityLog activity, Clock clock) {
        this.entries = entries;
        this.settingsRepository = settingsRepository;
        this.activity = activity;
        this.clock = clock;
    }

    // --- reads

    @Transactional(readOnly = true)
    public TaxSettings settings() {
        return settingsRepository.findById(TaxSettings.SINGLETON_ID).orElseGet(TaxSettings::new);
    }

    // one query for the whole year instead of four
    @Transactional(readOnly = true)
    public List<QuarterSummary> yearSummaries(int year) {
        List<TaxEntry> all = yearEntries(year);
        TaxSettings s = settings();
        List<QuarterSummary> result = new ArrayList<>();
        for (int q = 1; q <= 4; q++) result.add(TaxCalculator.compute(year, q, all, s));
        return result;
    }

    @Transactional(readOnly = true)
    public List<TaxEntry> entriesForQuarter(int year, int quarter) {
        return entries.findByDateBetweenOrderByDateDescIdDesc(
                TaxCalculator.quarterStart(year, quarter), TaxCalculator.quarterEnd(year, quarter));
    }

    @Transactional(readOnly = true)
    public long entryCount() {
        return entries.count();
    }

    @Transactional(readOnly = true)
    public List<CuotaMonth> cuotaMonths(int year) {
        Set<YearMonth> paid = settings().paidMonths();
        YearMonth now = YearMonth.now(clock);
        List<CuotaMonth> months = new ArrayList<>();
        for (Month m : Month.values()) {
            YearMonth ym = YearMonth.of(year, m);
            months.add(new CuotaMonth(ym, paid.contains(ym), ym.equals(now), ym.isAfter(now)));
        }
        return months;
    }

    @Transactional(readOnly = true)
    public BigDecimal cuotaPaidInYear(int year) {
        TaxSettings s = settings();
        long count = s.paidMonths().stream().filter(m -> m.getYear() == year).count();
        return s.getMonthlyCuota().multiply(BigDecimal.valueOf(count));
    }

    // --- writes

    public TaxEntry create(TaxEntryRequest r) {
        TaxEntry entry = new TaxEntry();
        apply(entry, r);
        entries.save(entry);
        boolean income = entry.isIncome();
        String who = entry.getCounterparty() == null ? "" : " - " + entry.getCounterparty();
        activity.record(entry.getDate(), income ? TransactionType.INCOME : TransactionType.EXPENSE,
                entry.getTotal(), "EUR", null, income ? "activity.invoice" : "activity.expense",
                entry.getConcept(), who);
        return entry;
    }

    public TaxEntry update(Long id, TaxEntryRequest r) {
        TaxEntry entry = get(id);
        apply(entry, r);
        return entry;
    }

    public void delete(Long id) {
        entries.delete(get(id));
    }

    public TaxSettings saveSettings(TaxSettingsRequest r) {
        TaxSettings s = settings();
        s.setMonthlyCuota(r.monthlyCuota().setScale(2, RoundingMode.HALF_UP));
        s.setDefaultRetentionRate(r.defaultRetentionRate());
        s.setCuotaDeductible(r.cuotaDeductible());
        s.setShowOnDashboard(r.showOnDashboard());
        return settingsRepository.save(s);
    }

    public TaxSettings setShowOnDashboard(boolean show) {
        TaxSettings s = settings();
        s.setShowOnDashboard(show);
        return settingsRepository.save(s);
    }

    // returns true if the month ends up marked as paid
    public boolean toggleCuota(YearMonth month) {
        TaxSettings s = settings();
        Set<YearMonth> paid = s.paidMonths();
        boolean nowPaid = !paid.remove(month);
        if (nowPaid) paid.add(month);
        s.setPaidMonths(paid);
        settingsRepository.save(s);
        return nowPaid;
    }

    // January up to the current month, used by the sample data
    public void markCuotaPaidToDate() {
        TaxSettings s = settings();
        Set<YearMonth> paid = s.paidMonths();
        YearMonth now = YearMonth.now(clock);
        for (int m = 1; m <= now.getMonthValue(); m++) paid.add(YearMonth.of(now.getYear(), m));
        s.setPaidMonths(paid);
        settingsRepository.save(s);
    }

    private TaxEntry get(Long id) {
        return entries.findById(id).orElseThrow(() -> new NotFoundException("error.entryNotFound"));
    }

    private void apply(TaxEntry entry, TaxEntryRequest r) {
        entry.setDate(r.date());
        entry.setConcept(r.concept().trim());
        entry.setCounterparty(blankToNull(r.counterparty()));
        entry.setCategory(blankToNull(r.category()));
        entry.setType(r.type());
        entry.setBaseAmount(r.baseAmount().setScale(2, RoundingMode.HALF_UP));
        entry.setIvaRate(r.ivaRate());
        if (r.type() == TaxEntryType.INGRESO) {
            entry.setIrpfRetentionRate(r.irpfRetentionRate() == null ? BigDecimal.ZERO : r.irpfRetentionRate());
            entry.setDeductible(false);
        } else {
            entry.setIrpfRetentionRate(BigDecimal.ZERO);
            entry.setDeductible(r.deductible());
        }
    }

    private List<TaxEntry> yearEntries(int year) {
        return entries.findByDateBetweenOrderByDateDescIdDesc(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
