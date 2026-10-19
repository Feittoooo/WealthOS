package com.wealthos.portfolio.dto;

import java.util.List;

// merged = the purchase went into an asset you already had
public record BuyResult(AssetView asset, boolean merged, List<String> warnings) {
}
