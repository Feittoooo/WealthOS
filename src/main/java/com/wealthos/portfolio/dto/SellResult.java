package com.wealthos.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SellResult(
        String ticker,
        LocalDate date,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal proceeds,
        BigDecimal realisedGain,
        boolean closed,
        boolean cashCredited) {
}
