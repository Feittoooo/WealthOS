package com.wealthos.portfolio.service;

import com.wealthos.common.error.NotFoundException;
import com.wealthos.portfolio.domain.Account;
import com.wealthos.portfolio.domain.Asset;
import com.wealthos.portfolio.domain.AssetLedger;
import com.wealthos.portfolio.domain.AssetOperation;
import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.dto.AccountView;
import com.wealthos.portfolio.dto.AssetDetail;
import com.wealthos.portfolio.dto.AssetView;
import com.wealthos.portfolio.dto.OperationView;
import com.wealthos.portfolio.dto.PortfolioOverview;
import com.wealthos.portfolio.dto.PortfolioSummary;
import com.wealthos.portfolio.dto.PriceSourceStatus;
import com.wealthos.portfolio.dto.Slice;
import com.wealthos.portfolio.price.PriceService;
import com.wealthos.portfolio.repository.AccountRepository;
import com.wealthos.portfolio.repository.AssetOperationRepository;
import com.wealthos.portfolio.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read side of the portfolio: totals, table rows, allocation slices and asset details. */
@Service
@Transactional(readOnly = true)
public class PortfolioQueryService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String[] ACCOUNT_PALETTE = {
            "#4a7fd1", "#3f9e8c", "#c9973a", "#8a6cc2", "#5b9bd5", "#c46a7c", "#7d8590", "#89a54e"
    };

    private final AccountRepository accounts;
    private final AssetRepository assets;
    private final AssetOperationRepository operations;
    private final PriceService prices;

    public PortfolioQueryService(AccountRepository accounts, AssetRepository assets,
                                 AssetOperationRepository operations, PriceService prices) {
        this.accounts = accounts;
        this.assets = assets;
        this.operations = operations;
        this.prices = prices;
    }

    public PortfolioOverview overview() {
        List<AssetView> rows = assetRows(assets.findAllWithOperations());
        List<AccountView> accountRows = accountRows(rows);
        return new PortfolioOverview(summary(rows), rows, accountRows, allocationByType(rows, false),
                allocationByAccount(accountRows), new PriceSourceStatus(prices.providerName(), prices.isAvailable()));
    }

    public PortfolioSummary summary() {
        return summary(assetRows(assets.findAllWithOperations()));
    }

    public List<Slice> allocationByType(boolean includeCash) {
        return allocationByType(assetRows(assets.findAllWithOperations()), includeCash);
    }

    public AssetDetail detail(Long assetId) {
        Asset asset = assets.findById(assetId).orElseThrow(() -> new NotFoundException("error.positionNotFound"));
        AssetView view = assetRows(assets.findAllWithOperations()).stream()
                .filter(v -> v.id().equals(assetId)).findFirst()
                .orElseGet(() -> AssetView.of(asset, BigDecimal.ZERO));

        AssetLedger ledger = asset.ledger();
        List<OperationView> ops = new ArrayList<>();
        for (AssetLedger.Line line : ledger.lines()) {
            ops.add(OperationView.of(line, counterpart(line.op())));
        }
        ops.sort(Comparator.comparing(OperationView::date).thenComparing(OperationView::id).reversed());
        return new AssetDetail(view, ops);
    }

    // the other fund of a transfer, so the list can say "to FUNDX" / "from FUNDY"
    private String counterpart(AssetOperation op) {
        if (op.getTransferId() == null) return null;
        return operations.findByTransferId(op.getTransferId()).stream()
                .filter(o -> !o.getId().equals(op.getId()))
                .map(o -> o.getAsset().getTicker())
                .findFirst().orElse(null);
    }

    private PortfolioSummary summary(List<AssetView> rows) {
        List<Account> all = accounts.findAll();
        List<AssetView> open = rows.stream().filter(AssetView::open).toList();
        BigDecimal cash = sum(all, Account::getBalance);
        BigDecimal invested = sum(open, AssetView::costBasis);
        BigDecimal value = sum(open, AssetView::marketValue);
        BigDecimal gain = value.subtract(invested);
        AssetView best = open.stream().max(Comparator.comparing(AssetView::gainPct)).orElse(null);
        AssetView worst = open.stream().min(Comparator.comparing(AssetView::gainPct)).orElse(null);
        return new PortfolioSummary(cash.add(value), cash, invested, value, gain, percentage(gain, invested),
                all.size(), open.size(), best, worst);
    }

    private List<AssetView> assetRows(List<Asset> all) {
        List<AssetView> plain = all.stream().map(a -> AssetView.of(a, BigDecimal.ZERO)).toList();
        BigDecimal total = sum(plain, AssetView::marketValue);
        return plain.stream()
                .map(v -> v.withWeight(percentage(v.marketValue(), total)))
                .sorted(Comparator.comparing(AssetView::open).thenComparing(AssetView::marketValue).reversed())
                .toList();
    }

    private List<AccountView> accountRows(List<AssetView> allAssets) {
        List<Account> all = accounts.findAllByOrderByNameAsc();
        Map<Long, List<AssetView>> byAccount = allAssets.stream()
                .filter(AssetView::open)
                .collect(Collectors.groupingBy(AssetView::accountId));

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Account a : all) {
            grandTotal = grandTotal.add(a.getBalance())
                    .add(sum(byAccount.getOrDefault(a.getId(), List.of()), AssetView::marketValue));
        }
        List<AccountView> rows = new ArrayList<>();
        for (Account a : all) {
            List<AssetView> held = byAccount.getOrDefault(a.getId(), List.of());
            BigDecimal value = sum(held, AssetView::marketValue);
            BigDecimal cost = sum(held, AssetView::costBasis);
            BigDecimal total = a.getBalance().add(value);
            rows.add(new AccountView(a.getId(), a.getName(), a.getType(), a.getInstitution(), a.getCurrency(),
                    a.getBalance(), value, cost, value.subtract(cost), held.size(), total,
                    percentage(total, grandTotal)));
        }
        rows.sort(Comparator.comparing(AccountView::total).reversed());
        return rows;
    }

    private List<Slice> allocationByType(List<AssetView> rows, boolean includeCash) {
        Map<AssetType, BigDecimal> totals = new EnumMap<>(AssetType.class);
        rows.forEach(r -> totals.merge(r.assetType(), r.marketValue(), BigDecimal::add));
        List<Slice> slices = new ArrayList<>();
        totals.forEach((type, value) -> {
            if (value.signum() > 0) slices.add(new Slice(type.getMessageKey(), null, value, type.getColor()));
        });
        if (includeCash) {
            BigDecimal cash = sum(accounts.findAll(), Account::getBalance);
            if (cash.signum() > 0) slices.add(new Slice("common.cash", null, cash, AssetType.CASH_COLOR));
        }
        slices.sort(Comparator.comparing(Slice::value).reversed());
        return slices;
    }

    // cash + assets per account, which is why it updates as soon as either side changes
    private List<Slice> allocationByAccount(List<AccountView> rows) {
        List<Slice> slices = new ArrayList<>();
        int i = 0;
        for (AccountView row : rows) {
            if (row.total().signum() > 0) {
                slices.add(new Slice(null, row.name(), row.total(), ACCOUNT_PALETTE[i++ % ACCOUNT_PALETTE.length]));
            }
        }
        return slices;
    }

    static <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> getter) {
        return items.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) return BigDecimal.ZERO;
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }
}
