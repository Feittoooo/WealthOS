package com.wealthos.tax.dto;

import com.wealthos.tax.domain.TaxEntryType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxEntryRequest(
        @NotNull(message = "{val.entry.date.required}")
        LocalDate date,

        @NotBlank(message = "{val.entry.concept.required}")
        @Size(max = 160, message = "{val.entry.concept.size}")
        String concept,

        @Size(max = 120, message = "{val.entry.counterparty.size}")
        String counterparty,

        @Size(max = 60, message = "{val.entry.category.size}")
        String category,

        @NotNull(message = "{val.entry.type.required}")
        TaxEntryType type,

        @NotNull(message = "{val.entry.amount.required}")
        @DecimalMin(value = "0", inclusive = false, message = "{val.entry.amount.positive}")
        BigDecimal baseAmount,

        @NotNull(message = "{val.entry.iva.required}")
        @DecimalMin(value = "0", message = "{val.entry.iva.range}")
        @DecimalMax(value = "100", message = "{val.entry.iva.range}")
        BigDecimal ivaRate,

        @DecimalMin(value = "0", message = "{val.entry.irpf.range}")
        @DecimalMax(value = "100", message = "{val.entry.irpf.range}")
        BigDecimal irpfRetentionRate,

        boolean deductible) {
}
