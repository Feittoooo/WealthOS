package com.wealthos.portfolio;

import com.wealthos.common.error.BusinessException;
import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.domain.OperationType;
import com.wealthos.portfolio.dto.AmountMode;
import com.wealthos.portfolio.dto.AssetDetail;
import com.wealthos.portfolio.dto.AssetUpdateRequest;
import com.wealthos.portfolio.dto.AssetView;
import com.wealthos.portfolio.dto.BuyResult;
import com.wealthos.portfolio.dto.OperationUpdateRequest;
import com.wealthos.portfolio.dto.PortfolioSummary;
import com.wealthos.portfolio.dto.SellRequest;
import com.wealthos.portfolio.domain.Account;
import com.wealthos.portfolio.repository.AssetRepository;
import com.wealthos.portfolio.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Default setup: no price source, so empty prices fall back to each other or are rejected.
@SpringBootTest
class AssetServiceTest extends PortfolioTestSupport {

    @Autowired
    AssetRepository repository;
    @Autowired
    TradeService trades;

    @BeforeEach
    void setUp() {
        freshBroker("1000");
    }

    // --- grouping

    @Test
    void buyingTheSameTickerAgainAddsToTheSameAsset() {
        AssetView first = buy("acme", "10", "100", "120");
        BuyResult second = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", "140", null, null, false);

        assertThat(second.merged()).isTrue();
        assertThat(second.asset().id()).isEqualTo(first.id());
        assertThat(repository.count()).isEqualTo(1);
        AssetView v = view(first.id());
        assertThat(v.quantity()).isEqualByComparingTo("20");
        assertThat(v.avgBuyPrice()).isEqualByComparingTo("120");
        assertThat(v.operationsCount()).isEqualTo(2);
    }

    @Test
    void buyingMoreTodayWithoutCurrentPriceKeepsThePriceTheAssetHas() {
        AssetView first = buy("ACME", "10", "100", "120");
        buy("ACME", AssetType.STOCK, AmountMode.UNITS, "1", "90", null, null, false);
        assertThat(view(first.id()).currentPrice()).isEqualByComparingTo("120");
    }

    @Test
    void theIsinFindsTheAssetEvenWithAnotherTicker() {
        BuyResult a = assets.buy(request(broker.getId(), "SP500", "ZZ00TEST0001", AssetType.FUND, AmountMode.UNITS, "5", "10", "10", null, false));
        BuyResult b = assets.buy(request(broker.getId(), "S&P", "zz00test0001", AssetType.FUND, AmountMode.UNITS, "5", "12", "12", null, false));
        assertThat(b.merged()).isTrue();
        assertThat(b.asset().id()).isEqualTo(a.asset().id());
        assertThat(view(a.asset().id()).quantity()).isEqualByComparingTo("10");
    }

    @Test
    void theSameTickerInAnotherAccountIsAnotherAsset() {
        buy("ACME", "1", "10", "10");
        Account other = newAccount("Other broker", "0");
        BuyResult r = assets.buy(request(other.getId(), "ACME", null, AssetType.STOCK, AmountMode.UNITS, "1", "10", "10", null, false));
        assertThat(r.merged()).isFalse();
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void summaryAddsUpEveryOperation() {
        buy("AAA", "10", "50", "60");
        buy("AAA", "10", "50", "60");
        buy("BBB", "4", "100", "100");
        PortfolioSummary s = query.summary();
        assertThat(s.positionsCount()).isEqualTo(2);
        assertThat(s.totalInvested()).isEqualByComparingTo("1400");
        assertThat(s.marketValue()).isEqualByComparingTo("1600");
        assertThat(s.netWorth()).isEqualByComparingTo("2600");
        assertThat(accounts.get(broker.getId()).getBalance()).isEqualByComparingTo("1000");
    }

    // --- prices and amounts

    @Test
    void moneyModeWorksOutTheUnits() {
        AssetView v = buy("FND", AssetType.FUND, AmountMode.MONEY, "1000", "20", "25", null, false).asset();
        assertThat(v.quantity()).isEqualByComparingTo("50");
        assertThat(v.costBasis()).isEqualByComparingTo("1000");
    }

    @Test
    void onlyCurrentPriceMeansCostEqualsValue() {
        BuyResult r = buy("DDD", AssetType.STOCK, AmountMode.UNITS, "5", null, "30", null, false);
        assertThat(r.asset().avgBuyPrice()).isEqualByComparingTo("30");
        assertThat(r.warnings()).contains("warning.costAssumed");
    }

    @Test
    void withoutAnyPriceAndNoSourceTheUserMustTypeOne() {
        assertThatThrownBy(() -> buy("FFF", "3", null, null)).hasMessage("error.priceRequired");
    }

    @Test
    void lockOnWithoutSourceWarns() {
        BuyResult r = buy("HHH", AssetType.ETF, AmountMode.UNITS, "2", "40", "999", null, true);
        assertThat(r.asset().autoPrice()).isTrue();
        assertThat(r.asset().currentPrice()).isEqualByComparingTo("40");
        assertThat(r.warnings()).contains("warning.noPriceSource");
    }

    // --- editing

    @Test
    void editingTheGroupKeepsTheLockAndRejectsDuplicates() {
        AssetView a = buy("AAA", "1", "10", "10");
        buy("BBB", "1", "10", "10");
        AssetDetail d = query.detail(assets.update(a.id(),
                new AssetUpdateRequest("AAA", "", "New name", AssetType.ETF, bd("15"), true, null)).getId());
        assertThat(d.asset().autoPrice()).isTrue();
        assertThat(d.asset().name()).isEqualTo("New name");
        assertThatThrownBy(() -> assets.update(a.id(), new AssetUpdateRequest("bbb", "", null, AssetType.ETF, null, false, null)))
                .hasMessage("error.assetDuplicate");
    }

    @Test
    void editingOneOperationChangesTheTotals() {
        AssetView a = buy("AAA", "10", "100", "100");
        buy("AAA", "10", "100", "100");
        Long opId = query.detail(a.id()).operations().get(0).id();
        assets.updateOperation(opId, new OperationUpdateRequest(LocalDate.now(), bd("20"), bd("130")));
        AssetView v = view(a.id());
        assertThat(v.quantity()).isEqualByComparingTo("30");
        assertThat(v.costBasis()).isEqualByComparingTo("3600");
    }

    @Test
    void anEditThatBreaksTheHistoryIsRejected() {
        AssetView a = buy("AAA", "10", "100", "100");
        trades.sell(a.id(), new SellRequest(AmountMode.UNITS, bd("8"), bd("100"), null, false));
        Long buyId = query.detail(a.id()).operations().stream().filter(o -> o.type() == OperationType.BUY).findFirst().orElseThrow().id();
        assertThatThrownBy(() -> assets.updateOperation(buyId, new OperationUpdateRequest(LocalDate.now(), bd("5"), bd("100"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("error.negativeHoldings");
    }

    @Test
    void deletingTheLastOperationRemovesTheAsset() {
        AssetView a = buy("AAA", "1", "10", "10");
        Long opId = query.detail(a.id()).operations().get(0).id();
        assertThat(assets.deleteOperation(opId)).isNull();
        assertThat(repository.findById(a.id())).isEmpty();
    }

    @Test
    void deletingAnAccountDeletesItsAssets() {
        buy("AAA", "1", "10", "10");
        accounts.delete(broker.getId());
        assertThat(repository.count()).isZero();
    }
}
