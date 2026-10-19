package com.wealthos.tax.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TaxSettingsRequest(
        @NotNull(message = "{val.settings.cuota.required}")
        @DecimalMin(value = "0", message = "{val.settings.cuota.negative}")
        BigDecimal monthlyCuota,

        @NotNull(message = "{val.settings.retention.required}")
        @DecimalMin(value = "0", message = "{val.settings.retention.range}")
        @DecimalMax(value = "100", message = "{val.settings.retention.range}")
        BigDecimal defaultRetentionRate,

        boolean cuotaDeductible,

        boolean showOnDashboard) {
}
