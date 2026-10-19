package com.wealthos.portfolio.price;

import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.domain.Asset;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Thin layer over the provider that knows about "today" and about the price we already store.
 * The rules for empty fields live in the services that call this; this class only answers
 * "what was the price of X on day Y, if anyone knows".
 */
@Service
public class PriceService {

    private final PriceProvider provider;
    private final Clock clock;

    public PriceService(PriceProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public boolean isToday(LocalDate date) {
        return date == null || date.equals(today());
    }

    public String providerName() {
        return provider.name();
    }

    public boolean isAvailable() {
        return provider.isAvailable();
    }

    public Optional<BigDecimal> latest(String symbol, AssetType type, String currency) {
        return provider.latest(symbol, type, currency).map(PriceQuote::price);
    }

    // today (or no date) means "latest"
    public Optional<BigDecimal> on(String symbol, AssetType type, String currency, LocalDate date) {
        if (isToday(date)) return latest(symbol, type, currency);
        return provider.on(symbol, type, currency, date).map(PriceQuote::price);
    }

    /**
     * Price of an asset you hold on a date. For today we fall back to the price we already
     * have stored, which is the best we know without a price source.
     */
    public Optional<BigDecimal> on(Asset p, LocalDate date) {
        Optional<BigDecimal> fromProvider = on(p.getLookupSymbol(), p.getAssetType(), p.getCurrency(), date);
        if (fromProvider.isPresent() || !isToday(date)) return fromProvider;
        return Optional.ofNullable(p.getCurrentPrice());
    }
}
