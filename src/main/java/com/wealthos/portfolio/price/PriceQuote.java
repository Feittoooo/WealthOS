package com.wealthos.portfolio.price;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceQuote(String symbol, BigDecimal price, String currency, LocalDate date, String source) {
}
