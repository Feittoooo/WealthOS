package com.wealthos.tax.api;

import com.wealthos.common.error.BusinessException;
import com.wealthos.tax.dto.QuarterSummary;
import com.wealthos.tax.dto.TaxEntryRequest;
import com.wealthos.tax.dto.TaxEntryView;
import com.wealthos.tax.dto.TaxOverview;
import com.wealthos.tax.dto.TaxSettingsRequest;
import com.wealthos.tax.dto.TaxSettingsView;
import com.wealthos.tax.service.FiscalCalendar;
import com.wealthos.tax.service.TaxCalculator;
import com.wealthos.tax.service.TaxService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tax")
public class TaxController {

    private final TaxService tax;
    private final FiscalCalendar calendar;
    private final Clock clock;

    public TaxController(TaxService tax, FiscalCalendar calendar, Clock clock) {
        this.tax = tax;
        this.calendar = calendar;
        this.clock = clock;
    }

    @GetMapping
    public TaxOverview overview(@RequestParam(required = false) Integer year,
                                @RequestParam(required = false) Integer quarter) {
        LocalDate today = LocalDate.now(clock);
        int y = year != null ? year : today.getYear();
        int q = quarter != null && quarter >= 1 && quarter <= 4 ? quarter : TaxCalculator.quarterOf(today);
        List<QuarterSummary> summaries = tax.yearSummaries(y);
        BigDecimal yearTotal = summaries.stream().map(QuarterSummary::totalToPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TaxOverview(y, q, today.getYear(), TaxCalculator.quarterOf(today), today,
                summaries.get(q - 1), summaries, yearTotal,
                tax.entriesForQuarter(y, q).stream().map(TaxEntryView::of).toList(),
                calendar.upcoming(6, true), TaxSettingsView.of(tax.settings()),
                tax.cuotaMonths(y), tax.cuotaPaidInYear(y));
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public TaxEntryView create(@Valid @RequestBody TaxEntryRequest request) {
        return TaxEntryView.of(tax.create(request));
    }

    @PutMapping("/entries/{id}")
    public TaxEntryView update(@PathVariable Long id, @Valid @RequestBody TaxEntryRequest request) {
        return TaxEntryView.of(tax.update(id, request));
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tax.delete(id);
    }

    @PutMapping("/settings")
    public TaxSettingsView settings(@Valid @RequestBody TaxSettingsRequest request) {
        return TaxSettingsView.of(tax.saveSettings(request));
    }

    // the "show on dashboard" switch on the tax page
    @PutMapping("/settings/show-on-dashboard")
    public TaxSettingsView showOnDashboard(@RequestBody Map<String, Boolean> body) {
        return TaxSettingsView.of(tax.setShowOnDashboard(Boolean.TRUE.equals(body.get("show"))));
    }

    @PostMapping("/cuota/{month}/toggle")
    public Map<String, Boolean> toggleCuota(@PathVariable String month) {
        try {
            return Map.of("paid", tax.toggleCuota(YearMonth.parse(month)));
        } catch (DateTimeParseException e) {
            throw new BusinessException("error.invalidMonth", month);
        }
    }
}
