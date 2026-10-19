package com.wealthos.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// costCarried: purchase cost that moves to the destination fund (a traspaso defers the tax)
public record TransferResult(
        String originTicker,
        String destinationTicker,
        Long destinationAssetId,
        LocalDate date,
        BigDecimal unitsOut,
        BigDecimal value,
        BigDecimal unitsIn,
        BigDecimal costCarried,
        boolean originClosed) {
}
