package com.wealthos.portfolio.dto;

import java.util.List;

// Everything the investments page needs in one call.
public record PortfolioOverview(
        PortfolioSummary summary,
        List<AssetView> assets,
        List<AccountView> accounts,
        List<Slice> byType,
        List<Slice> byAccount,
        PriceSourceStatus priceSource) {
}
