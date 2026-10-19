package com.wealthos.portfolio.dto;

import com.wealthos.portfolio.domain.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A purchase. If the account already holds the same asset (same ISIN, or same ticker) it's
 * added to it; otherwise a new asset is created. Prices and date are optional, see AssetService.
 * With amountMode MONEY, amount is the money invested.
 */
public record BuyRequest(
        @NotNull(message = "{val.position.account.required}")
        Long accountId,

        @NotBlank(message = "{val.position.ticker.required}")
        @Size(max = 20, message = "{val.position.ticker.size}")
        String ticker,

        @Pattern(regexp = "^$|^[A-Za-z]{2}[A-Za-z0-9]{9}[0-9]$", message = "{val.position.isin.pattern}")
        String isin,

        @Size(max = 120, message = "{val.position.name.size}")
        String name,

        @NotNull(message = "{val.position.type.required}")
        AssetType assetType,

        AmountMode amountMode,

        @NotNull(message = "{val.position.amount.required}")
        @DecimalMin(value = "0", inclusive = false, message = "{val.position.amount.positive}")
        BigDecimal amount,

        @DecimalMin(value = "0", inclusive = false, message = "{val.position.avg.positive}")
        BigDecimal price,

        @DecimalMin(value = "0", inclusive = false, message = "{val.position.price.positive}")
        BigDecimal currentPrice,

        @PastOrPresent(message = "{val.date.future}")
        LocalDate date,

        boolean autoPrice) {

    public AmountMode mode() {
        return amountMode == null ? AmountMode.UNITS : amountMode;
    }
}
