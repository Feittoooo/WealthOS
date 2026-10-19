package com.wealthos.portfolio;

import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.dto.AmountMode;
import com.wealthos.portfolio.dto.AssetView;
import com.wealthos.portfolio.dto.SellRequest;
import com.wealthos.portfolio.price.PriceRefreshJob;
import com.wealthos.portfolio.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How things behave once a real price source exists (issue #1). The fake provider plays its
 * part, which also proves the rest of the code is ready for it.
 */
@SpringBootTest(properties = "wealthos.prices.provider=fake")
class WithPriceSourceTest extends PortfolioTestSupport {

    @TestConfiguration
    static class Prices {
        @Bean
        FakePriceProvider fakePriceProvider() {
            return new FakePriceProvider();
        }
    }

    @Autowired
    FakePriceProvider fake;
    @Autowired
    TradeService trades;
    @Autowired
    PriceRefreshJob refresh;

    private final LocalDate bought = LocalDate.now().minusMonths(3);

    @BeforeEach
    void setUp() {
        freshBroker("0");
        fake.clear();
        fake.latest("ACME", "120");
        fake.on("ACME", bought, "90");
    }

    @Test
    void emptyPricesWithADateComeFromThatDateAndToday() {
        AssetView a = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", null, null, bought, false).asset();
        assertThat(a.avgBuyPrice()).isEqualByComparingTo("90");
        assertThat(a.currentPrice()).isEqualByComparingTo("120");
    }

    @Test
    void everythingEmptyMeansBoughtTodaySoCostEqualsValue() {
        AssetView a = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", null, null, null, false).asset();
        assertThat(a.avgBuyPrice()).isEqualByComparingTo("120");
        assertThat(a.gain()).isEqualByComparingTo("0");
    }

    @Test
    void lockOnTakesTheSourcePriceOverATypedOne() {
        AssetView a = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "1", "100", "5", null, true).asset();
        assertThat(a.currentPrice()).isEqualByComparingTo("120");
    }

    @Test
    void dailyRefreshOnlyTouchesLockedAssets() {
        AssetView locked = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "1", "100", null, null, true).asset();
        AssetView manual = buy("OTHR", AssetType.STOCK, AmountMode.UNITS, "1", "100", "100", null, false).asset();
        fake.latest("ACME", "130");
        fake.latest("OTHR", "130");

        assertThat(refresh.run().updated()).isEqualTo(1);
        assertThat(view(locked.id()).currentPrice()).isEqualByComparingTo("130");
        assertThat(view(manual.id()).currentPrice()).isEqualByComparingTo("100");
    }

    @Test
    void saleOnAPastDateWithoutPriceUsesThatDatesPrice() {
        AssetView a = buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", "80", "120", bought.minusDays(1), false).asset();
        var r = trades.sell(a.id(), new SellRequest(AmountMode.UNITS, bd("2"), null, bought, false));
        assertThat(r.price()).isEqualByComparingTo("90");
        assertThat(r.realisedGain()).isEqualByComparingTo("20");
    }
}
