package com.wealthos.portfolio.domain;

public enum AccountType {
    BANK("bank"),
    BROKERAGE("briefcase"),
    CRYPTO_WALLET("coins"),
    CASH("banknote");

    private final String icon;

    AccountType(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public String getMessageKey() {
        return "accountType." + name();
    }
}
