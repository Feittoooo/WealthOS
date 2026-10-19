package com.wealthos.portfolio.dto;

import com.wealthos.portfolio.domain.AccountType;

import java.math.BigDecimal;

public record AccountView(
        Long id,
        String name,
        AccountType type,
        String institution,
        String currency,
        BigDecimal cashBalance,
        BigDecimal positionsValue,
        BigDecimal positionsCost,
        BigDecimal positionsGain,
        long positionsCount,
        BigDecimal total,
        BigDecimal weight) {
}
