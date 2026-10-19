package com.wealthos.portfolio.dto;

import java.math.BigDecimal;

// totalInvested = cost basis of open positions. netWorth = cash + market value.
public record PortfolioSummary(
        BigDecimal netWorth,
        BigDecimal totalCash,
        BigDecimal totalInvested,
        BigDecimal marketValue,
        BigDecimal totalReturn,
        BigDecimal totalReturnPct,
        long accountsCount,
        long positionsCount,
        AssetView bestPerformer,
        AssetView worstPerformer) {
}
