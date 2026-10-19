package com.wealthos.portfolio.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCurveTest {

    @Test
    void drawsAStraightLineBetweenKnownPricesAndStaysFlatOutside() {
        PriceCurve curve = new PriceCurve();
        curve.add(LocalDate.of(2026, 1, 1), new BigDecimal("100"));
        curve.add(LocalDate.of(2026, 1, 11), new BigDecimal("200"));

        assertThat(curve.at(LocalDate.of(2025, 12, 1))).isEqualByComparingTo("100");
        assertThat(curve.at(LocalDate.of(2026, 1, 6))).isEqualByComparingTo("150");
        assertThat(curve.at(LocalDate.of(2026, 1, 11))).isEqualByComparingTo("200");
        assertThat(curve.at(LocalDate.of(2026, 3, 1))).isEqualByComparingTo("200");
    }
}
