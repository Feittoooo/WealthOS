package com.wealthos.activity;

public enum TransactionType {
    DEPOSIT(1),
    WITHDRAWAL(-1),
    BUY(-1),
    SELL(1),
    DIVIDEND(1),
    INCOME(1),
    EXPENSE(-1),
    // fund to fund switch (traspaso): no money in or out, no tax event
    TRANSFER(0),
    ADJUSTMENT(0);

    // +1 money in, -1 money out. Only used to colour the activity feed.
    private final int direction;

    TransactionType(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }

    public String getMessageKey() {
        return "txType." + name();
    }
}
