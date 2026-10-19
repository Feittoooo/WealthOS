package com.wealthos.portfolio.price;

import com.wealthos.portfolio.domain.AssetType;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Where market prices come from.
 *
 * Only {@link NoPriceProvider} exists for now. Issue #1 adds a Yahoo Finance one: implement this
 * interface, annotate it with {@code @ConditionalOnProperty(name = "wealthos.prices.provider",
 * havingValue = "yahoo")} and everything else (the lock, empty prices, sells and transfers on a
 * past date, the daily refresh) starts using it without further changes.
 */
public interface PriceProvider {

    /** Short id shown in the UI and the debug page, e.g. "none" or "yahoo". */
    String name();

    /** False means every lookup will come back empty, so the UI can say so up front. */
    boolean isAvailable();

    Optional<PriceQuote> latest(String symbol, AssetType type, String currency);

    /** Closing price on that day, or the last one before it (weekends, holidays). */
    Optional<PriceQuote> on(String symbol, AssetType type, String currency, LocalDate date);
}
