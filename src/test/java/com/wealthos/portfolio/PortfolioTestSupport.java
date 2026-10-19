package com.wealthos.portfolio;

import com.wealthos.data.DataService;
import com.wealthos.portfolio.domain.Account;
import com.wealthos.portfolio.domain.AccountType;
import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.dto.AccountRequest;
import com.wealthos.portfolio.dto.AmountMode;
import com.wealthos.portfolio.dto.AssetView;
import com.wealthos.portfolio.dto.BuyRequest;
import com.wealthos.portfolio.dto.BuyResult;
import com.wealthos.portfolio.service.AccountService;
import com.wealthos.portfolio.service.AssetService;
import com.wealthos.portfolio.service.PortfolioQueryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

// Small builders so the tests read like the scenario they check.
abstract class PortfolioTestSupport {

    @Autowired
    protected AccountService accounts;
    @Autowired
    protected AssetService assets;
    @Autowired
    protected PortfolioQueryService query;
    @Autowired
    protected DataService data;

    protected Account broker;

    protected void freshBroker(String cash) {
        data.deleteAll();
        broker = newAccount("Test broker", cash);
    }

    protected Account newAccount(String name, String cash) {
        return accounts.create(new AccountRequest(name, AccountType.BROKERAGE, null, "EUR", new BigDecimal(cash)));
    }

    protected BuyResult buy(String ticker, AssetType type, AmountMode mode, String amount,
                            String price, String current, LocalDate date, boolean lock) {
        return assets.buy(request(broker.getId(), ticker, null, type, mode, amount, price, current, date, lock));
    }

    protected AssetView buy(String ticker, String units, String price, String current) {
        return buy(ticker, AssetType.STOCK, AmountMode.UNITS, units, price, current, null, false).asset();
    }

    protected AssetView buyFund(String ticker, String units, String price, String current, LocalDate date) {
        return buy(ticker, AssetType.FUND, AmountMode.UNITS, units, price, current, date, false).asset();
    }

    protected BuyRequest request(Long accountId, String ticker, String isin, AssetType type, AmountMode mode,
                                 String amount, String price, String current, LocalDate date, boolean lock) {
        return new BuyRequest(accountId, ticker, isin, null, type, mode, new BigDecimal(amount),
                price == null ? null : new BigDecimal(price), current == null ? null : new BigDecimal(current), date, lock);
    }

    protected AssetView view(Long assetId) {
        return query.detail(assetId).asset();
    }

    protected static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
