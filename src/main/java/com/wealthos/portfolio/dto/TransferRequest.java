package com.wealthos.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fund switch (traspaso). Goes either into a fund you already hold (destinationAssetId)
 * or into a new one (newFund). Prices are optional, same as in a sale.
 */
public record TransferRequest(
        @NotNull(message = "{val.amountMode.required}")
        AmountMode mode,

        @DecimalMin(value = "0", inclusive = false, message = "{val.transfer.amount.positive}")
        BigDecimal amount,

        @PastOrPresent(message = "{val.date.future}")
        LocalDate date,

        @DecimalMin(value = "0", inclusive = false, message = "{val.transfer.price.positive}")
        BigDecimal originPrice,

        Long destinationAssetId,

        @Valid
        NewFund newFund,

        @DecimalMin(value = "0", inclusive = false, message = "{val.transfer.price.positive}")
        BigDecimal destinationPrice) {

    public record NewFund(
            @NotNull(message = "{val.position.account.required}")
            Long accountId,

            @NotBlank(message = "{val.position.ticker.required}")
            @Size(max = 20, message = "{val.position.ticker.size}")
            String ticker,

            @Pattern(regexp = "^$|^[A-Za-z]{2}[A-Za-z0-9]{9}[0-9]$",
                    message = "{val.position.isin.pattern}")
            String isin,

            @Size(max = 120, message = "{val.position.name.size}")
            String name,

            boolean autoPrice,

            @Size(max = 40, message = "{val.position.priceSymbol.size}")
            String priceSymbol) {
    }
}
