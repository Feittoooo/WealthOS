package com.wealthos.portfolio;

import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.domain.OperationType;
import com.wealthos.portfolio.dto.AmountMode;
import com.wealthos.portfolio.dto.AssetDetail;
import com.wealthos.portfolio.dto.AssetView;
import com.wealthos.portfolio.dto.BuyRequest;
import com.wealthos.portfolio.dto.OperationUpdateRequest;
import com.wealthos.portfolio.dto.OperationView;
import com.wealthos.portfolio.dto.SellRequest;
import com.wealthos.portfolio.dto.SellResult;
import com.wealthos.portfolio.dto.TransferRequest;
import com.wealthos.portfolio.dto.TransferResult;
import com.wealthos.portfolio.service.TradeService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TradeServiceTest extends PortfolioTestSupport {

    @Autowired
    TradeService trades;
    @Autowired
    Validator validator;

    private final LocalDate monthsAgo = LocalDate.now().minusMonths(3);

    @BeforeEach
    void setUp() {
        freshBroker("1000");
    }

    private SellRequest sell(AmountMode mode, String amount, String price, LocalDate date, boolean credit) {
        return new SellRequest(mode, amount == null ? null : bd(amount), price == null ? null : bd(price), date, credit);
    }

    private TransferRequest toAsset(AssetView dest, AmountMode mode, String amount, String originPrice, String destPrice) {
        return new TransferRequest(mode, amount == null ? null : bd(amount), null,
                originPrice == null ? null : bd(originPrice), dest.id(), null, destPrice == null ? null : bd(destPrice));
    }

    // --- sales

    @Test
    void partialSaleKeepsAverageCostAndLeavesCashAlone() {
        AssetView a = buy("ACME", "10", "50", "70");
        SellResult r = trades.sell(a.id(), sell(AmountMode.UNITS, "4", "80", null, false));

        assertThat(r.realisedGain()).isEqualByComparingTo("120");
        AssetView after = view(a.id());
        assertThat(after.quantity()).isEqualByComparingTo("6");
        assertThat(after.avgBuyPrice()).isEqualByComparingTo("50");
        assertThat(after.realisedGain()).isEqualByComparingTo("120");
        assertThat(accounts.get(broker.getId()).getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void proceedsGoToCashOnlyWhenAsked() {
        AssetView a = buy("ACME", "10", "50", "70");
        trades.sell(a.id(), sell(AmountMode.UNITS, "4", "80", null, true));
        assertThat(accounts.get(broker.getId()).getBalance()).isEqualByComparingTo("1320");
    }

    @Test
    void sellingEverythingKeepsTheAssetWithItsHistory() {
        AssetView a = buy("ACME", "2.5", "10", "12");
        SellResult r = trades.sell(a.id(), sell(AmountMode.ALL, null, "12", null, false));
        assertThat(r.closed()).isTrue();
        AssetDetail d = query.detail(a.id());
        assertThat(d.asset().open()).isFalse();
        assertThat(d.operations()).hasSize(2);
    }

    @Test
    void sellByMoneyAndWithTodaysStoredPrice() {
        AssetView a = buy("ACME", "10", "50", "65");
        assertThat(trades.sell(a.id(), sell(AmountMode.MONEY, "130", null, null, false)).quantity()).isEqualByComparingTo("2");
    }

    @Test
    void pastDateWithoutPriceNeedsAPriceWhileThereIsNoSource() {
        AssetView a = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", "50", "65", monthsAgo, false).asset();
        assertThatThrownBy(() -> trades.sell(a.id(), sell(AmountMode.UNITS, "1", null, LocalDate.now().minusDays(5), false)))
                .hasMessage("error.priceOnDateRequired");
    }

    @Test
    void cannotSellMoreThanHeldThatDayOrBeforeTheFirstPurchase() {
        AssetView a = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "1", "10", "10", monthsAgo, false).asset();
        buy("ACME", "5", "10", "10"); // bought today
        // a month ago only 1 unit was held
        assertThatThrownBy(() -> trades.sell(a.id(), sell(AmountMode.UNITS, "2", "10", LocalDate.now().minusMonths(1), false)))
                .hasMessage("error.sellTooMuch");
        assertThatThrownBy(() -> trades.sell(a.id(), sell(AmountMode.ALL, null, "10", monthsAgo.minusDays(1), false)))
                .hasMessage("error.dateBeforePurchase");
    }

    // --- gains: the edge cases from issue #10

    @Test
    void cryptoWithEightDecimalsRoundsOnlyTheMoney() {
        AssetView a = buy("ORBC", AssetType.CRYPTO, AmountMode.UNITS, "0.12345678", "30123.45", null, null, false).asset();
        SellResult r = trades.sell(a.id(), sell(AmountMode.UNITS, "0.03333333", "33000", null, false));

        // units are never rounded; money is rounded to cents only when shown or realised
        assertThat(r.realisedGain()).isEqualByComparingTo("95.88");
        AssetView after = view(a.id());
        assertThat(after.quantity()).isEqualByComparingTo("0.09012345");
        assertThat(after.costBasis()).isEqualByComparingTo("2714.83");
        assertThat(after.avgBuyPrice()).isEqualByComparingTo("30123.45");

        // and selling the rest leaves nothing behind, not even dust
        trades.sell(a.id(), sell(AmountMode.ALL, null, "33000", null, false));
        assertThat(view(a.id()).quantity()).isEqualByComparingTo("0");
        assertThat(view(a.id()).costBasis()).isEqualByComparingTo("0");
        assertThat(view(a.id()).open()).isFalse();
    }

    @Test
    void sellingAtTheAveragePriceRealisesExactlyZero() {
        AssetView a = buy("ACME", "10", "100", "110");
        buy("ACME", "10", "120", "110"); // average 110
        SellResult r = trades.sell(a.id(), sell(AmountMode.UNITS, "7", "110", null, false));

        assertThat(r.realisedGain()).isEqualByComparingTo("0");
        assertThat(r.realisedGain().toPlainString()).isEqualTo("0.00");
        assertThat(view(a.id()).avgBuyPrice()).isEqualByComparingTo("110");
    }

    // Decided: a price of 0 stays rejected. For a gift or an airdrop you enter its market value
    // on the day you got it, which is also roughly the acquisition value Hacienda uses.
    @Test
    void aPurchaseOrEditAtPriceZeroIsRejected() {
        BuyRequest free = request(broker.getId(), "AIR", null, AssetType.CRYPTO, AmountMode.UNITS, "100", "0", "0", null, false);
        assertThat(messages(validator.validate(free)))
                .containsExactlyInAnyOrder("{val.position.avg.positive}", "{val.position.price.positive}");

        OperationUpdateRequest edit = new OperationUpdateRequest(LocalDate.now(), bd("1"), bd("0"));
        assertThat(messages(validator.validate(edit))).containsExactly("{val.position.avg.positive}");
    }

    // Decided: editing an old purchase changes the gain of the sales after it. The average cost
    // at the time of a sale depends on every purchase before it, so a corrected purchase has to
    // give a corrected gain; keeping the old one would leave the totals inconsistent.
    @Test
    void editingAnOldPurchaseRecalculatesTheGainOfLaterSales() {
        AssetView a = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", "50", "80", monthsAgo, false).asset();
        trades.sell(a.id(), sell(AmountMode.UNITS, "5", "80", null, false));
        assertThat(view(a.id()).realisedGain()).isEqualByComparingTo("150");

        OperationView purchase = operation(a, OperationType.BUY);
        assets.updateOperation(purchase.id(), new OperationUpdateRequest(monthsAgo, bd("10"), bd("60")));

        assertThat(operation(a, OperationType.SELL).realisedGain()).isEqualByComparingTo("100");
        assertThat(view(a.id()).realisedGain()).isEqualByComparingTo("100");
        assertThat(view(a.id()).costBasis()).isEqualByComparingTo("300");
    }

    // Decided: no RealizedGain table. Each sale's gain is worked out from the operations, so it
    // can't go stale after an edit; a report for the Renta can read it from there.
    @Test
    void eachSaleCarriesItsOwnGainAndTheyAddUp() {
        AssetView a = buy("ACME", "10", "50", "50");
        trades.sell(a.id(), sell(AmountMode.UNITS, "2", "60", null, false));
        trades.sell(a.id(), sell(AmountMode.UNITS, "3", "40", null, false));

        List<OperationView> sales = query.detail(a.id()).operations().stream()
                .filter(o -> o.type() == OperationType.SELL).toList();
        assertThat(sales).extracting(OperationView::realisedGain)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(bd("20"), bd("-30"));
        assertThat(view(a.id()).realisedGain()).isEqualByComparingTo("-10");
    }

    private OperationView operation(AssetView asset, OperationType type) {
        return query.detail(asset.id()).operations().stream().filter(o -> o.type() == type).findFirst().orElseThrow();
    }

    private static List<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessageTemplate).toList();
    }

    // --- transfers

    @Test
    void transferToAFundYouHoldCarriesTheCostAndRealisesNothing() {
        AssetView origin = buyFund("ORIG", "20", "100", "150", null);
        AssetView dest = buyFund("DEST", "10", "20", "30", null);

        TransferResult r = trades.transfer(origin.id(), toAsset(dest, AmountMode.UNITS, "10", null, null));

        assertThat(r.value()).isEqualByComparingTo("1500");
        assertThat(r.unitsIn()).isEqualByComparingTo("50");
        assertThat(r.costCarried()).isEqualByComparingTo("1000");
        AssetView d = view(dest.id());
        assertThat(d.quantity()).isEqualByComparingTo("60");
        assertThat(d.avgBuyPrice()).isEqualByComparingTo("20");
        assertThat(view(origin.id()).quantity()).isEqualByComparingTo("10");
        assertThat(view(origin.id()).realisedGain()).isEqualByComparingTo("0");
    }

    @Test
    void transferIntoANewFundCreatesItAsAFund() {
        AssetView origin = buyFund("ORIG", "8", "50", "75", null);
        TransferResult r = trades.transfer(origin.id(), new TransferRequest(AmountMode.ALL, null, null, null, null,
                new TransferRequest.NewFund(broker.getId(), "newf", null, "New fund", false, null), bd("12")));

        assertThat(r.originClosed()).isTrue();
        AssetView created = view(r.destinationAssetId());
        assertThat(created.ticker()).isEqualTo("NEWF");
        assertThat(created.quantity()).isEqualByComparingTo("50");
        assertThat(created.costBasis()).isEqualByComparingTo("400");
    }

    @Test
    void editingAnOriginPurchaseUpdatesTheCostCarriedToTheDestination() {
        AssetView origin = buyFund("ORIG", "10", "100", "100", monthsAgo);
        AssetView dest = buyFund("DEST", "1", "10", "10", monthsAgo);
        trades.transfer(origin.id(), toAsset(dest, AmountMode.ALL, null, "100", "10"));
        assertThat(view(dest.id()).costBasis()).isEqualByComparingTo("1010");

        OperationView originalBuy = query.detail(origin.id()).operations().stream()
                .filter(o -> o.type() == OperationType.BUY).findFirst().orElseThrow();
        assets.updateOperation(originalBuy.id(), new OperationUpdateRequest(monthsAgo, bd("10"), bd("80")));

        assertThat(view(dest.id()).costBasis()).isEqualByComparingTo("810");
    }

    @Test
    void deletingATransferRemovesBothLegs() {
        AssetView origin = buyFund("ORIG", "10", "100", "100", null);
        AssetView dest = buyFund("DEST", "1", "10", "10", null);
        trades.transfer(origin.id(), toAsset(dest, AmountMode.UNITS, "5", null, null));

        OperationView in = query.detail(dest.id()).operations().stream()
                .filter(o -> o.type() == OperationType.TRANSFER_IN).findFirst().orElseThrow();
        assertThat(in.counterpart()).isEqualTo("ORIG");
        assets.deleteOperation(in.id());

        assertThat(view(origin.id()).quantity()).isEqualByComparingTo("10");
        assertThat(view(dest.id()).quantity()).isEqualByComparingTo("1");
    }

    @Test
    void onlyFundsCanBeTransferred() {
        AssetView stock = buy("STK", "1", "10", "10");
        AssetView fund = buyFund("FND", "1", "10", "10", null);
        assertThatThrownBy(() -> trades.transfer(stock.id(), toAsset(fund, AmountMode.ALL, null, null, null)))
                .hasMessage("error.transferFundsOnly");
        assertThatThrownBy(() -> trades.transfer(fund.id(), toAsset(fund, AmountMode.ALL, null, null, null)))
                .hasMessage("error.transferSameFund");
    }
}
