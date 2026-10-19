package com.wealthos.portfolio;

import com.wealthos.common.error.BusinessException;
import com.wealthos.portfolio.domain.AssetLedger;
import com.wealthos.portfolio.domain.AssetOperation;
import com.wealthos.portfolio.domain.OperationType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetLedgerTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 10);
    private static final LocalDate MAR = LocalDate.of(2026, 3, 10);
    private static final LocalDate JUN = LocalDate.of(2026, 6, 10);

    private static AssetOperation op(OperationType type, LocalDate date, String units, String price) {
        return new AssetOperation(type, date, new BigDecimal(units), new BigDecimal(price));
    }

    @Test
    void purchasesAddUpWithAnAveragePrice() {
        AssetLedger l = AssetLedger.of(List.of(op(OperationType.BUY, JAN, "10", "100"), op(OperationType.BUY, MAR, "10", "120")));
        assertThat(l.units()).isEqualByComparingTo("20");
        assertThat(l.cost()).isEqualByComparingTo("2200");
        assertThat(l.averagePrice()).isEqualByComparingTo("110");
        assertThat(l.firstDate()).isEqualTo(JAN);
        assertThat(l.lastDate()).isEqualTo(MAR);
    }

    @Test
    void aSaleRealisesAgainstTheAverageAndKeepsItForTheRest() {
        AssetLedger l = AssetLedger.of(List.of(
                op(OperationType.BUY, JAN, "10", "100"),
                op(OperationType.BUY, MAR, "10", "120"),
                op(OperationType.SELL, JUN, "5", "150")));
        assertThat(l.units()).isEqualByComparingTo("15");
        assertThat(l.averagePrice()).isEqualByComparingTo("110");
        assertThat(l.realisedGain()).isEqualByComparingTo("200"); // 5 x (150 - 110)
    }

    @Test
    void ordersByDateWhateverTheInputOrder() {
        AssetLedger l = AssetLedger.of(List.of(op(OperationType.SELL, JUN, "5", "150"), op(OperationType.BUY, JAN, "10", "100")));
        assertThat(l.units()).isEqualByComparingTo("5");
    }

    @Test
    void knowsWhatWasHeldOnAnyDay() {
        AssetLedger l = AssetLedger.of(List.of(
                op(OperationType.BUY, JAN, "10", "100"),
                op(OperationType.BUY, MAR, "10", "120"),
                op(OperationType.SELL, JUN, "5", "150")));
        assertThat(l.unitsAt(JAN.minusDays(1))).isEqualByComparingTo("0");
        assertThat(l.unitsAt(JAN)).isEqualByComparingTo("10");
        assertThat(l.unitsAt(MAR.plusDays(5))).isEqualByComparingTo("20");
        assertThat(l.unitsAt(JUN)).isEqualByComparingTo("15");
        assertThat(l.costAt(MAR.minusDays(1))).isEqualByComparingTo("1000");
    }

    @Test
    void refusesToSellUnitsThatWerentThereYet() {
        assertThatThrownBy(() -> AssetLedger.of(List.of(op(OperationType.BUY, MAR, "10", "100"), op(OperationType.SELL, JAN, "1", "100"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("error.negativeHoldings");
    }

    @Test
    void transferInUsesTheCarriedCostNotTheMarketPrice() {
        AssetOperation in = op(OperationType.TRANSFER_IN, JAN, "50", "30");
        in.setCarriedCost(new BigDecimal("1000"));
        AssetLedger l = AssetLedger.of(List.of(in));
        assertThat(l.cost()).isEqualByComparingTo("1000");
        assertThat(l.realisedGain()).isEqualByComparingTo("0");
    }

    @Test
    void transferOutMovesCostWithoutRealising() {
        AssetOperation out = op(OperationType.TRANSFER_OUT, MAR, "5", "200");
        AssetLedger l = AssetLedger.of(List.of(op(OperationType.BUY, JAN, "10", "100"), out));
        assertThat(l.costMovedBy(out)).isEqualByComparingTo("500");
        assertThat(l.realisedGain()).isEqualByComparingTo("0");
        assertThat(l.cost()).isEqualByComparingTo("500");
    }

    @Test
    void sellingEverythingLeavesItClosed() {
        AssetLedger l = AssetLedger.of(List.of(op(OperationType.BUY, JAN, "0.3", "10"), op(OperationType.SELL, MAR, "0.3", "12")));
        assertThat(l.isOpen()).isFalse();
        assertThat(l.cost()).isEqualByComparingTo("0");
    }
}
