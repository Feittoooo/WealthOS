package com.wealthos.portfolio;

import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.dto.AmountMode;
import com.wealthos.portfolio.dto.NetWorthSeries;
import com.wealthos.portfolio.repository.AccountRepository;
import com.wealthos.portfolio.service.NetWorthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// The chart is rebuilt from the operations, so a purchase dated in the past shows up back then.
@SpringBootTest
class NetWorthServiceTest extends PortfolioTestSupport {

    @Autowired
    NetWorthService netWorth;
    @Autowired
    AccountRepository accountRepository;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        freshBroker("0");
        // the account has existed for three years
        var account = accountRepository.findById(broker.getId()).orElseThrow();
        account.setCreatedAt(LocalDateTime.now().minusYears(3));
        accountRepository.save(account);
    }

    private BigDecimal valueOn(NetWorthSeries s, LocalDate day) {
        return s.points().stream().filter(p -> !p.date().isAfter(day)).reduce((a, b) -> b).orElseThrow().value();
    }

    @Test
    void aPurchaseTypedTodayButDatedEarlierAppearsFromItsDate() {
        LocalDate bought = today.minusMonths(6);
        buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", "100", "160", bought, false);

        NetWorthSeries s = netWorth.series(NetWorthService.Range.Y1, null, null);

        assertThat(s.estimated()).isTrue();
        assertThat(valueOn(s, bought.minusDays(10))).isEqualByComparingTo("0");
        // on the purchase date: 10 x 100; today: 10 x 160; in between, a straight line
        assertThat(s.points().stream().filter(p -> p.date().isAfter(bought) && p.date().isBefore(today))
                .allMatch(p -> p.value().compareTo(new BigDecimal("1000")) >= 0
                        && p.value().compareTo(new BigDecimal("1600")) <= 0)).isTrue();
        assertThat(s.points().get(s.points().size() - 1).value()).isEqualByComparingTo("1600");
        assertThat(s.points().get(s.points().size() - 1).invested()).isEqualByComparingTo("1000");
    }

    @Test
    void laterPurchasesAndSalesChangeTheUnitsFromTheirDate() {
        buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", "100", "100", today.minusMonths(8), false);
        buy("ACME", AssetType.STOCK, AmountMode.UNITS, "10", "100", "100", today.minusMonths(4), false);

        NetWorthSeries s = netWorth.series(NetWorthService.Range.Y1, null, null);
        assertThat(valueOn(s, today.minusMonths(6))).isEqualByComparingTo("1000");
        assertThat(valueOn(s, today.minusMonths(2))).isEqualByComparingTo("2000");
    }

    @Test
    void rangesSetTheStartAndTheStep() {
        buy("ACME", AssetType.STOCK, AmountMode.UNITS, "1", "100", "100", today.minusYears(2), false);

        NetWorthSeries all = netWorth.series(NetWorthService.Range.ALL, null, null);
        assertThat(all.from()).isEqualTo(today.minusYears(3));
        assertThat(netWorth.series(NetWorthService.Range.YTD, null, null).from()).isEqualTo(today.withDayOfYear(1));
        assertThat(netWorth.series(NetWorthService.Range.Y5, null, null).step()).isEqualTo("month");

        NetWorthSeries custom = netWorth.series(NetWorthService.Range.CUSTOM, today.minusDays(30), today.minusDays(10));
        assertThat(custom.step()).isEqualTo("day");
        assertThat(custom.points()).hasSize(21);
        assertThat(custom.to()).isEqualTo(today.minusDays(10));
    }
}
