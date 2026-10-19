package com.wealthos.portfolio.dto;

import java.util.List;

// What opens when you click an asset: the group plus every operation, newest first.
public record AssetDetail(AssetView asset, List<OperationView> operations) {
}
