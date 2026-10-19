package com.wealthos.portfolio.dto;

import com.wealthos.portfolio.domain.Asset;
import com.wealthos.portfolio.domain.AssetLedger;
import com.wealthos.portfolio.domain.AssetType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row of the assets table: the whole group, with everything added up from its operations. */
public record AssetView(
        Long id,
        Long accountId,
        String accountName,
        String ticker,
        String isin,
        String name,
        AssetType assetType,
        String currency,
        BigDecimal quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal costBasis,
        BigDecimal marketValue,
        BigDecimal gain,
        BigDecimal gainPct,
        BigDecimal realisedGain,
        BigDecimal weight,
        LocalDate firstDate,
        LocalDate lastDate,
        int operationsCount,
        boolean open,
        boolean autoPrice,
        String priceSymbol,
        LocalDateTime priceUpdatedAt) {

    public AssetView withWeight(BigDecimal w) {
        return new AssetView(id, accountId, accountName, ticker, isin, name, assetType, currency, quantity, avgBuyPrice,
                currentPrice, costBasis, marketValue, gain, gainPct, realisedGain, w, firstDate, lastDate,
                operationsCount, open, autoPrice, priceSymbol, priceUpdatedAt);
    }

    public static AssetView of(Asset a, BigDecimal weight) {
        AssetLedger ledger = a.ledger();
        BigDecimal units = ledger.units();
        BigDecimal cost = ledger.cost();
        BigDecimal value = units.multiply(a.getCurrentPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gain = value.subtract(cost);
        BigDecimal pct = cost.signum() == 0 ? BigDecimal.ZERO
                : gain.multiply(BigDecimal.valueOf(100)).divide(cost, 2, RoundingMode.HALF_UP);
        return new AssetView(a.getId(), a.getAccount().getId(), a.getAccount().getName(), a.getTicker(), a.getIsin(),
                a.getName(), a.getAssetType(), a.getCurrency(), units, ledger.averagePrice(), a.getCurrentPrice(),
                cost, value, gain, pct, ledger.realisedGain(), weight, ledger.firstDate(), ledger.lastDate(),
                a.getOperations().size(), ledger.isOpen(), a.isAutoPrice(), a.getPriceSymbol(), a.getPriceUpdatedAt());
    }
}
