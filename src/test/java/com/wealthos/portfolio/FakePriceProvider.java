package com.wealthos.portfolio;

import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.price.PriceProvider;
import com.wealthos.portfolio.price.PriceQuote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Stands in for the future Yahoo provider: prices are whatever the test puts in. */
public class FakePriceProvider implements PriceProvider {

    private final Map<String, BigDecimal> latest = new HashMap<>();
    private final Map<String, BigDecimal> historic = new HashMap<>();

    public void latest(String symbol, String price) {
        latest.put(symbol, new BigDecimal(price));
    }

    public void on(String symbol, LocalDate date, String price) {
        historic.put(symbol + "@" + date, new BigDecimal(price));
    }

    public void clear() {
        latest.clear();
        historic.clear();
    }

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<PriceQuote> latest(String symbol, AssetType type, String currency) {
        return Optional.ofNullable(latest.get(symbol))
                .map(p -> new PriceQuote(symbol, p, currency, LocalDate.now(), "fake"));
    }

    @Override
    public Optional<PriceQuote> on(String symbol, AssetType type, String currency, LocalDate date) {
        return Optional.ofNullable(historic.get(symbol + "@" + date))
                .map(p -> new PriceQuote(symbol, p, currency, date, "fake"));
    }
}
