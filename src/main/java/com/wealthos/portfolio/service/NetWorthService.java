package com.wealthos.portfolio.service;

import com.wealthos.portfolio.domain.Account;
import com.wealthos.portfolio.domain.Asset;
import com.wealthos.portfolio.domain.AssetLedger;
import com.wealthos.portfolio.domain.AssetOperation;
import com.wealthos.portfolio.domain.OperationType;
import com.wealthos.portfolio.dto.NetWorthSeries;
import com.wealthos.portfolio.price.PriceService;
import com.wealthos.portfolio.repository.AccountRepository;
import com.wealthos.portfolio.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Net worth on any day, rebuilt from the operations: units held that day x the price that day,
 * plus cash. So a purchase dated in March shows up in March even if you typed it in today.
 *
 * Prices: from the price source when there is one, otherwise estimated with PriceCurve.
 * Cash: an account counts from the day it was created, at its current balance, minus sale
 * proceeds that were added to it later. Other cash movements over time aren't tracked yet.
 */
@Service
@Transactional(readOnly = true)
public class NetWorthService {

    public enum Range { ALL, Y5, Y3, Y1, YTD, CUSTOM }

    private final AccountRepository accounts;
    private final AssetRepository assets;
    private final PriceService prices;
    private final Clock clock;

    public NetWorthService(AccountRepository accounts, AssetRepository assets, PriceService prices, Clock clock) {
        this.accounts = accounts;
        this.assets = assets;
        this.prices = prices;
        this.clock = clock;
    }

    public NetWorthSeries series(Range range, LocalDate customFrom, LocalDate customTo) {
        LocalDate today = LocalDate.now(clock);
        Snapshot data = load();
        LocalDate to = range == Range.CUSTOM && customTo != null && !customTo.isAfter(today) ? customTo : today;
        LocalDate from = switch (range) {
            case Y5 -> to.minusYears(5);
            case Y3 -> to.minusYears(3);
            case Y1 -> to.minusYears(1);
            case YTD -> to.withDayOfYear(1);
            case CUSTOM -> customFrom != null ? customFrom : data.start(today);
            case ALL -> data.start(today);
        };
        if (from.isAfter(to)) from = to;

        long days = ChronoUnit.DAYS.between(from, to);
        String step = days <= 92 ? "day" : days <= 3 * 366 ? "week" : "month";
        List<NetWorthSeries.Point> points = new ArrayList<>();
        for (LocalDate d : dates(from, to, step)) {
            points.add(new NetWorthSeries.Point(d, data.valueAt(d, today), data.investedAt(d)));
        }
        return new NetWorthSeries(from, to, step, !prices.isAvailable(), points);
    }

    public BigDecimal current() {
        LocalDate today = LocalDate.now(clock);
        return load().valueAt(today, today);
    }

    // compared with the last day of the previous month; null when there was nothing back then
    public BigDecimal changeSinceLastMonth() {
        LocalDate today = LocalDate.now(clock);
        Snapshot data = load();
        LocalDate lastMonth = today.withDayOfMonth(1).minusDays(1);
        if (data.start(today).isAfter(lastMonth)) return null;
        return data.valueAt(today, today).subtract(data.valueAt(lastMonth, today));
    }

    private Snapshot load() {
        return new Snapshot(accounts.findAll(), assets.findAllWithOperations());
    }

    private static List<LocalDate> dates(LocalDate from, LocalDate to, String step) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate d = from;
        while (d.isBefore(to)) {
            list.add(d);
            d = switch (step) {
                case "day" -> d.plusDays(1);
                case "week" -> d.plusWeeks(1);
                // month ends, the usual way of plotting monthly balances
                default -> d.equals(d.with(TemporalAdjusters.lastDayOfMonth()))
                        ? d.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
                        : d.with(TemporalAdjusters.lastDayOfMonth());
            };
        }
        list.add(to);
        return list;
    }

    /** Everything needed to value the portfolio on any day, loaded once per request. */
    private final class Snapshot {
        private final List<Account> accountList;
        private final List<Holding> holdings = new ArrayList<>();

        Snapshot(List<Account> accountList, List<Asset> assetList) {
            this.accountList = accountList;
            for (Asset a : assetList) holdings.add(new Holding(a));
        }

        LocalDate start(LocalDate today) {
            LocalDate start = today;
            for (Holding h : holdings) {
                LocalDate first = h.ledger.firstDate();
                if (first != null && first.isBefore(start)) start = first;
            }
            for (Account a : accountList) {
                LocalDate created = a.getCreatedAt().toLocalDate();
                if (created.isBefore(start)) start = created;
            }
            return start;
        }

        BigDecimal valueAt(LocalDate day, LocalDate today) {
            BigDecimal total = cashAt(day);
            for (Holding h : holdings) {
                BigDecimal units = h.ledger.unitsAt(day);
                if (units.signum() == 0) continue;
                total = total.add(units.multiply(h.priceAt(day, today)));
            }
            return total.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal investedAt(LocalDate day) {
            BigDecimal total = cashAt(day);
            for (Holding h : holdings) total = total.add(h.ledger.costAt(day));
            return total.setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal cashAt(LocalDate day) {
            BigDecimal cash = BigDecimal.ZERO;
            for (Account a : accountList) {
                if (a.getCreatedAt().toLocalDate().isAfter(day)) continue;
                cash = cash.add(a.getBalance());
            }
            // proceeds credited after that day weren't in the account yet
            for (Holding h : holdings) {
                for (AssetOperation op : h.asset.getOperations()) {
                    if (op.getType() == OperationType.SELL && op.isCashCredited() && op.getDate().isAfter(day)
                            && !h.asset.getAccount().getCreatedAt().toLocalDate().isAfter(day)) {
                        cash = cash.subtract(op.getAmount());
                    }
                }
            }
            return cash;
        }
    }

    private final class Holding {
        final Asset asset;
        final AssetLedger ledger;
        final PriceCurve curve = new PriceCurve();

        Holding(Asset asset) {
            this.asset = asset;
            this.ledger = asset.ledger();
            for (AssetOperation op : asset.getOperations()) curve.add(op.getDate(), op.getPrice());
            curve.add(LocalDate.now(clock), asset.getCurrentPrice());
        }

        BigDecimal priceAt(LocalDate day, LocalDate today) {
            if (!day.isBefore(today)) return asset.getCurrentPrice();
            if (prices.isAvailable()) {
                return prices.on(asset, day).orElseGet(() -> curve.at(day));
            }
            return curve.at(day);
        }
    }
}
