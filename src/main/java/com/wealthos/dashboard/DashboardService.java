package com.wealthos.dashboard;

import com.wealthos.activity.ActivityLog;
import com.wealthos.data.DataService;
import com.wealthos.portfolio.dto.PortfolioSummary;
import com.wealthos.portfolio.service.NetWorthService;
import com.wealthos.portfolio.service.PortfolioQueryService;
import com.wealthos.tax.service.FiscalCalendar;
import com.wealthos.tax.service.TaxService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Puts together the home page from the other modules. Holds no logic of its own. */
@Service
public class DashboardService {

    private final PortfolioQueryService portfolio;
    private final NetWorthService netWorth;
    private final ActivityLog activity;
    private final TaxService tax;
    private final FiscalCalendar calendar;
    private final DataService data;

    public DashboardService(PortfolioQueryService portfolio, NetWorthService netWorth, ActivityLog activity,
                            TaxService tax, FiscalCalendar calendar, DataService data) {
        this.portfolio = portfolio;
        this.netWorth = netWorth;
        this.activity = activity;
        this.tax = tax;
        this.calendar = calendar;
        this.data = data;
    }

    public DashboardView build() {
        PortfolioSummary summary = portfolio.summary();
        BigDecimal change = netWorth.changeSinceLastMonth();
        BigDecimal previous = change == null ? null : summary.netWorth().subtract(change);
        BigDecimal changePct = previous == null || previous.signum() == 0 ? null
                : change.multiply(BigDecimal.valueOf(100)).divide(previous.abs(), 2, RoundingMode.HALF_UP);
        boolean showTax = tax.settings().isShowOnDashboard();

        return new DashboardView(data.isEmpty(), summary, change, changePct,
                portfolio.allocationByType(true), activity.latest(5), showTax,
                showTax ? calendar.upcoming(3, true) : List.of());
    }
}
