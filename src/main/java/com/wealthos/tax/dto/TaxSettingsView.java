package com.wealthos.tax.dto;

import com.wealthos.tax.domain.TaxSettings;

import java.math.BigDecimal;

public record TaxSettingsView(BigDecimal monthlyCuota, BigDecimal defaultRetentionRate,
                              boolean cuotaDeductible, boolean showOnDashboard) {

    public static TaxSettingsView of(TaxSettings s) {
        return new TaxSettingsView(s.getMonthlyCuota(), s.getDefaultRetentionRate(),
                s.isCuotaDeductible(), s.isShowOnDashboard());
    }
}
