package com.wealthos.portfolio.price;

import com.wealthos.portfolio.domain.Asset;
import com.wealthos.portfolio.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Updates the current price of every locked (autoPrice) asset. Runs every weekday evening. */
@Component
public class PriceRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshJob.class);

    private final AssetRepository assets;
    private final PriceService prices;
    private final Clock clock;

    public PriceRefreshJob(AssetRepository assets, PriceService prices, Clock clock) {
        this.assets = assets;
        this.prices = prices;
        this.clock = clock;
    }

    public record Result(String provider, int checked, int updated, int missing) {
    }

    @Scheduled(cron = "${wealthos.prices.refresh-cron:0 30 22 * * MON-FRI}")
    public void scheduled() {
        run();
    }

    @Transactional
    public Result run() {
        List<Asset> locked = assets.findByAutoPriceTrue();
        if (!prices.isAvailable()) {
            log.debug("Price refresh skipped: no price source ({} locked assets)", locked.size());
            return new Result(prices.providerName(), locked.size(), 0, locked.size());
        }
        int updated = 0;
        int missing = 0;
        for (Asset p : locked) {
            Optional<BigDecimal> price = prices.latest(p.getLookupSymbol(), p.getAssetType(), p.getCurrency());
            if (price.isEmpty()) {
                missing++;
                log.warn("No price for {} ({})", p.getTicker(), p.getLookupSymbol());
                continue;
            }
            if (price.get().compareTo(p.getCurrentPrice()) != 0) {
                p.setCurrentPrice(price.get());
                updated++;
            }
            p.setPriceUpdatedAt(LocalDateTime.now(clock));
        }
        log.info("Price refresh: {} locked, {} updated, {} without price", locked.size(), updated, missing);
        return new Result(prices.providerName(), locked.size(), updated, missing);
    }
}
