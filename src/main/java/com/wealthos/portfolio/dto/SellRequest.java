package com.wealthos.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

// price and date are optional: no date means today, no price means the price on that date
public record SellRequest(
        @NotNull(message = "{val.amountMode.required}")
        AmountMode mode,

        @DecimalMin(value = "0", inclusive = false, message = "{val.sell.amount.positive}")
        BigDecimal amount,

        @DecimalMin(value = "0", inclusive = false, message = "{val.sell.price.positive}")
        BigDecimal price,

        @PastOrPresent(message = "{val.date.future}")
        LocalDate date,

        boolean creditCash) {
}
