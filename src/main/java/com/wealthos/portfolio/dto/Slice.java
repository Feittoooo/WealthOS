package com.wealthos.portfolio.dto;

import java.math.BigDecimal;

// key is a message key when the label has to be translated (asset types, cash); otherwise label is used as is
public record Slice(String key, String label, BigDecimal value, String color) {
}
