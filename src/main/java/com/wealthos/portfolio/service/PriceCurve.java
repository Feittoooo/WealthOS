package com.wealthos.portfolio.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Price of an asset on any day, from the few prices we actually know (each operation's price
 * and today's). Between two known prices it draws a straight line; before the first and after
 * the last it stays flat. It's an estimate, used until a price source gives real history.
 */
final class PriceCurve {

    private final NavigableMap<LocalDate, BigDecimal> known = new TreeMap<>();

    void add(LocalDate date, BigDecimal price) {
        if (date != null && price != null) known.put(date, price);
    }

    BigDecimal at(LocalDate day) {
        if (known.isEmpty()) return BigDecimal.ZERO;
        Map.Entry<LocalDate, BigDecimal> before = known.floorEntry(day);
        Map.Entry<LocalDate, BigDecimal> after = known.ceilingEntry(day);
        if (before == null) return after.getValue();
        if (after == null || before.getKey().equals(after.getKey())) return before.getValue();

        long span = ChronoUnit.DAYS.between(before.getKey(), after.getKey());
        long elapsed = ChronoUnit.DAYS.between(before.getKey(), day);
        BigDecimal step = after.getValue().subtract(before.getValue())
                .multiply(BigDecimal.valueOf(elapsed))
                .divide(BigDecimal.valueOf(span), MathContext.DECIMAL64);
        return before.getValue().add(step);
    }
}
