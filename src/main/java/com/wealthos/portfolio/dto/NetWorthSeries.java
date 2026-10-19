package com.wealthos.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Net worth over a period, rebuilt from the operations. estimated = true means prices between
 * the known ones (each operation's price and today's) are interpolated, because there's no
 * price source with real historical prices yet.
 */
public record NetWorthSeries(LocalDate from, LocalDate to, String step, boolean estimated, List<Point> points) {

    public record Point(LocalDate date, BigDecimal value, BigDecimal invested) {
    }
}
