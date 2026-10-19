package com.wealthos.dashboard;

import com.wealthos.activity.ActivityItem;
import com.wealthos.portfolio.dto.PortfolioSummary;
import com.wealthos.portfolio.dto.Slice;
import com.wealthos.tax.dto.FiscalDeadline;

import java.math.BigDecimal;
import java.util.List;

/** deadlines is empty when the user hid the tax widgets from the tax page. */
public record DashboardView(
        boolean empty,
        PortfolioSummary summary,
        BigDecimal changeSinceLastMonth,
        BigDecimal changeSinceLastMonthPct,
        List<Slice> allocation,
        List<ActivityItem> recentActivity,
        boolean showTax,
        List<FiscalDeadline> deadlines) {
}
