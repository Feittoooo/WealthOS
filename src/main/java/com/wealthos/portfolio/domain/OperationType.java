package com.wealthos.portfolio.domain;

public enum OperationType {
    BUY(1),
    SELL(-1),
    // the two legs of a fund transfer (traspaso), linked by transferId
    TRANSFER_IN(1),
    TRANSFER_OUT(-1);

    // +1 adds units to the asset, -1 takes them away
    private final int sign;

    OperationType(int sign) {
        this.sign = sign;
    }

    public int getSign() {
        return sign;
    }

    public boolean isTransfer() {
        return this == TRANSFER_IN || this == TRANSFER_OUT;
    }
}
