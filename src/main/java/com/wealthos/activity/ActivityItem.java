package com.wealthos.activity;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ActivityItem(
        Long id,
        LocalDate date,
        TransactionType type,
        int direction,
        String messageCode,
        List<Object> args,
        String description,
        BigDecimal amount,
        String currency,
        String accountName) {

    public static ActivityItem of(Transaction t) {
        return new ActivityItem(t.getId(), t.getDate(), t.getType(), t.getType().getDirection(),
                t.getMessageCode(), List.of(t.getArgs()), t.getDescription(), t.getAmount(),
                t.getCurrency(), t.getAccountName());
    }
}
