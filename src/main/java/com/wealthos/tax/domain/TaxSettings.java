package com.wealthos.tax.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

// Always a single row (id 1).
@Entity
@Table(name = "tax_settings")
public class TaxSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "monthly_cuota", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyCuota = new BigDecimal("230.00");

    // 15 general, 7 during the first years of activity
    @Column(name = "default_retention_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultRetentionRate = BigDecimal.valueOf(15);

    @Column(name = "cuota_deductible", nullable = false)
    private boolean cuotaDeductible = true;

    // whether the tax widgets appear on the dashboard; off unless the user turns it on.
    // (renamed from show_on_dashboard so existing databases start hidden as well)
    @Column(name = "show_tax_on_dashboard", nullable = false, columnDefinition = "boolean default false")
    private boolean showOnDashboard;

    // "2026-01,2026-02,..." - good enough for twelve flags a year
    @Column(name = "paid_cuota_months", length = 2000)
    private String paidCuotaMonths = "";

    public Set<YearMonth> paidMonths() {
        if (paidCuotaMonths == null || paidCuotaMonths.isBlank()) return new TreeSet<>();
        return Arrays.stream(paidCuotaMonths.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(YearMonth::parse)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public void setPaidMonths(Set<YearMonth> months) {
        this.paidCuotaMonths = new TreeSet<>(months).stream()
                .map(YearMonth::toString)
                .collect(Collectors.joining(","));
    }

    public Long getId() { return id; }

    public BigDecimal getMonthlyCuota() { return monthlyCuota; }
    public void setMonthlyCuota(BigDecimal monthlyCuota) { this.monthlyCuota = monthlyCuota; }

    public BigDecimal getDefaultRetentionRate() { return defaultRetentionRate; }
    public void setDefaultRetentionRate(BigDecimal defaultRetentionRate) { this.defaultRetentionRate = defaultRetentionRate; }

    public boolean isCuotaDeductible() { return cuotaDeductible; }
    public void setCuotaDeductible(boolean cuotaDeductible) { this.cuotaDeductible = cuotaDeductible; }

    public boolean isShowOnDashboard() { return showOnDashboard; }
    public void setShowOnDashboard(boolean showOnDashboard) { this.showOnDashboard = showOnDashboard; }

    public String getPaidCuotaMonths() { return paidCuotaMonths; }
}
