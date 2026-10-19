package com.wealthos.portfolio.price;

import com.wealthos.portfolio.domain.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

// Default until there's a real source: never answers, so prices have to be typed in.
@Component
@ConditionalOnProperty(name = "wealthos.prices.provider", havingValue = "none", matchIfMissing = true)
public class NoPriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(NoPriceProvider.class);

    @Override
    public String name() {
        return "none";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<PriceQuote> latest(String symbol, AssetType type, String currency) {
        log.debug("No price source configured, latest price of {} unknown", symbol);
        return Optional.empty();
    }

    @Override
    public Optional<PriceQuote> on(String symbol, AssetType type, String currency, LocalDate date) {
        log.debug("No price source configured, price of {} on {} unknown", symbol, date);
        return Optional.empty();
    }
}
