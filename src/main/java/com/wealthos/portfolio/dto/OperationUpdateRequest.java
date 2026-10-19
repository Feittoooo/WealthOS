package com.wealthos.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperationUpdateRequest(
        @NotNull(message = "{val.entry.date.required}")
        @PastOrPresent(message = "{val.date.future}")
        LocalDate date,

        @NotNull(message = "{val.position.amount.required}")
        @DecimalMin(value = "0", inclusive = false, message = "{val.position.amount.positive}")
        BigDecimal units,

        @NotNull(message = "{val.operation.price.required}")
        @DecimalMin(value = "0", inclusive = false, message = "{val.position.avg.positive}")
        BigDecimal price) {
}
