package com.wealthos.portfolio.domain;

public enum AssetType {
    STOCK("#6E79F6"),
    ETF("#2EC4A6"),
    FUND("#F5A524"),
    CRYPTO("#B47CFF"),
    OTHER("#8B93A7");

    // cash isn't an asset type but gets its own slice on the dashboard donut
    public static final String CASH_COLOR = "#4BA3E3";

    private final String color;

    AssetType(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    public String getMessageKey() {
        return "assetType." + name();
    }
}
