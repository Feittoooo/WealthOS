package com.wealthos.portfolio.service;

import com.wealthos.activity.ActivityLog;
import com.wealthos.activity.TransactionType;
import com.wealthos.common.error.BusinessException;
import com.wealthos.common.error.NotFoundException;
import com.wealthos.common.format.NumberFormats;
import com.wealthos.portfolio.domain.Account;
import com.wealthos.portfolio.domain.Asset;
import com.wealthos.portfolio.domain.AssetLedger;
import com.wealthos.portfolio.domain.AssetOperation;
import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.domain.OperationType;
import com.wealthos.portfolio.dto.AmountMode;
import com.wealthos.portfolio.dto.AssetUpdateRequest;
import com.wealthos.portfolio.dto.AssetView;
import com.wealthos.portfolio.dto.BuyRequest;
import com.wealthos.portfolio.dto.BuyResult;
import com.wealthos.portfolio.dto.OperationUpdateRequest;
import com.wealthos.portfolio.price.PriceService;
import com.wealthos.portfolio.repository.AssetOperationRepository;
import com.wealthos.portfolio.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Buying, editing and deleting. An asset is the group (one per thing you hold in an account),
 * each buy, sale or transfer is an operation inside it.
 *
 * A purchase of something the account already holds (same ISIN, or same ticker if there's no
 * ISIN) is added to that asset instead of creating a new one.
 *
 * Empty fields on a purchase (the price source answers only once issue #1 is done):
 *  - no date: today
 *  - no price: the price on that date
 *  - no current price, or the lock is on: today's price (for an asset already held, the one it has)
 *  - if only one of the two prices can be worked out, the other takes the same value
 *    (cost = market value). With nothing at all to go on, the user has to type a price.
 */
@Service
@Transactional
public class AssetService {

    private final AssetRepository assets;
    private final AssetOperationRepository operations;
    private final AccountService accounts;
    private final PriceService prices;
    private final ActivityLog activity;
    private final NumberFormats formats;
    private final Clock clock;

    public AssetService(AssetRepository assets, AssetOperationRepository operations, AccountService accounts,
                        PriceService prices, ActivityLog activity, NumberFormats formats, Clock clock) {
        this.assets = assets;
        this.operations = operations;
        this.accounts = accounts;
        this.prices = prices;
        this.activity = activity;
        this.formats = formats;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Asset get(Long id) {
        return assets.findById(id).orElseThrow(() -> new NotFoundException("error.positionNotFound"));
    }

    // --- buying

    public BuyResult buy(BuyRequest r) {
        if (r.mode() == AmountMode.ALL) throw new BusinessException("error.amountModeAll");
        Account account = accounts.get(r.accountId());
        String ticker = r.ticker().trim().toUpperCase(Locale.ROOT);
        String isin = blankToNull(r.isin()) == null ? null : r.isin().trim().toUpperCase(Locale.ROOT);
        LocalDate date = r.date() != null ? r.date() : today();

        Asset existing = findSame(account.getId(), ticker, isin);
        String symbol = existing != null ? existing.getLookupSymbol() : isin != null ? isin : ticker;
        AssetType type = existing != null ? existing.getAssetType() : r.assetType();
        String currency = account.getCurrency();
        List<String> warnings = new ArrayList<>();

        BigDecimal price = r.price() != null ? r.price()
                : existing != null ? prices.on(existing, date).orElse(null)
                : prices.on(symbol, type, currency, date).orElse(null);
        BigDecimal latest = prices.latest(symbol, type, currency).orElse(null);
        if (r.autoPrice() && !prices.isAvailable()) warnings.add("warning.noPriceSource");

        BigDecimal current;
        if (r.autoPrice()) {
            current = latest != null ? latest : existing != null ? existing.getCurrentPrice() : null;
        } else if (r.currentPrice() != null) {
            current = r.currentPrice();
        } else if (latest != null) {
            current = latest;
        } else if (existing != null) {
            // buying more of something already held doesn't change what it's worth today
            current = existing.getCurrentPrice();
        } else {
            current = null;
        }

        if (price == null && current != null) {
            price = current;
            warnings.add("warning.costAssumed");
        }
        if (price == null) {
            throw new BusinessException(prices.isAvailable() ? "error.priceNotFound" : "error.priceRequired", ticker);
        }
        if (current == null) current = price;

        Asset asset = existing;
        if (asset == null) {
            asset = new Asset();
            asset.setAccount(account);
            asset.setTicker(ticker);
            asset.setIsin(isin);
            asset.setName(blankToNull(r.name()) == null ? ticker : r.name().trim());
            asset.setAssetType(r.assetType());
        } else if (asset.getIsin() == null && isin != null) {
            asset.setIsin(isin);
        }
        asset.setAutoPrice(r.autoPrice());
        if (asset.getCurrentPrice() == null || current.compareTo(asset.getCurrentPrice()) != 0) {
            asset.setCurrentPrice(current);
            asset.setPriceUpdatedAt(LocalDateTime.now(clock));
        }

        BigDecimal units = r.mode() == AmountMode.MONEY ? r.amount().divide(price, 8, RoundingMode.HALF_DOWN) : r.amount();
        AssetOperation op = new AssetOperation(OperationType.BUY, date, units, price);
        asset.addOperation(op);
        asset.ledger(); // a purchase can't break the history, but keep the check in one place
        // an asset already loaded must get the new operation persisted directly (save() would merge a copy)
        if (existing != null) operations.save(op);
        else assets.save(asset);

        activity.record(date, TransactionType.BUY, op.getAmount(), currency, account.getName(), "activity.bought",
                formats.qty(units), ticker, formats.price(price, currency));
        return new BuyResult(AssetView.of(asset, BigDecimal.ZERO), existing != null, warnings);
    }

    // --- editing the group

    public Asset update(Long id, AssetUpdateRequest r) {
        Asset asset = get(id);
        String ticker = r.ticker().trim().toUpperCase(Locale.ROOT);
        String isin = blankToNull(r.isin()) == null ? null : r.isin().trim().toUpperCase(Locale.ROOT);
        Asset clash = findSame(asset.getAccount().getId(), ticker, isin);
        if (clash != null && !clash.getId().equals(asset.getId())) {
            throw new BusinessException("error.assetDuplicate", ticker);
        }
        asset.setTicker(ticker);
        asset.setIsin(isin);
        asset.setName(blankToNull(r.name()) == null ? ticker : r.name().trim());
        asset.setAssetType(r.assetType());
        asset.setAutoPrice(r.autoPrice());
        asset.setPriceSymbol(blankToNull(r.priceSymbol()));

        BigDecimal current = r.autoPrice()
                ? prices.latest(asset.getLookupSymbol(), asset.getAssetType(), asset.getCurrency()).orElse(asset.getCurrentPrice())
                : r.currentPrice() != null ? r.currentPrice() : asset.getCurrentPrice();
        if (current.compareTo(asset.getCurrentPrice()) != 0) {
            asset.setCurrentPrice(current);
            asset.setPriceUpdatedAt(LocalDateTime.now(clock));
        }
        return asset;
    }

    /** Deletes the asset and its history. Transfers to or from it lose their other leg too. */
    public void delete(Long id) {
        Asset asset = get(id);
        Set<Asset> touched = new HashSet<>();
        for (AssetOperation op : new ArrayList<>(asset.getOperations())) {
            if (op.getTransferId() == null) continue;
            for (AssetOperation other : operations.findByTransferId(op.getTransferId())) {
                if (other.getAsset().getId().equals(asset.getId())) continue;
                other.getAsset().removeOperation(other);
                touched.add(other.getAsset());
            }
        }
        assets.delete(asset);
        touched.forEach(this::syncTransfers);
    }

    // --- single operations

    public Asset updateOperation(Long opId, OperationUpdateRequest r) {
        AssetOperation op = operation(opId);
        if (op.getType().isTransfer()) throw new BusinessException("error.transferEdit");
        op.setDate(r.date());
        op.setUnits(r.units());
        op.setPrice(r.price());
        Asset asset = op.getAsset();
        syncTransfers(asset); // also checks the history still makes sense
        return asset;
    }

    /** Deletes one operation (both legs if it's a transfer). Returns null if the asset is left empty. */
    public Asset deleteOperation(Long opId) {
        AssetOperation op = operation(opId);
        Asset asset = op.getAsset();
        List<AssetOperation> legs = op.getTransferId() == null ? List.of(op) : operations.findByTransferId(op.getTransferId());
        Set<Asset> touched = new HashSet<>();
        for (AssetOperation leg : legs) {
            leg.getAsset().removeOperation(leg);
            touched.add(leg.getAsset());
        }
        for (Asset a : touched) {
            if (a.getOperations().isEmpty()) {
                assets.delete(a);
            } else {
                syncTransfers(a);
            }
        }
        return asset.getOperations().isEmpty() ? null : asset;
    }

    /**
     * After any change, the cost a transfer carried to the other fund may be different (it
     * depends on what was bought before). This recalculates it and follows the chain.
     * Throws if the history no longer adds up (e.g. selling units you didn't have yet).
     */
    void syncTransfers(Asset start) {
        Set<Long> seen = new HashSet<>();
        List<Asset> queue = new ArrayList<>(List.of(start));
        while (!queue.isEmpty()) {
            Asset asset = queue.remove(0);
            if (asset.getId() != null && !seen.add(asset.getId())) continue;
            AssetLedger ledger = asset.ledger();
            for (AssetOperation op : asset.getOperations()) {
                if (op.getType() != OperationType.TRANSFER_OUT || op.getTransferId() == null) continue;
                BigDecimal moved = ledger.costMovedBy(op);
                for (AssetOperation in : operations.findByTransferId(op.getTransferId())) {
                    if (in.getType() != OperationType.TRANSFER_IN) continue;
                    if (in.getCarriedCost() == null || moved == null || in.getCarriedCost().compareTo(moved) != 0) {
                        in.setCarriedCost(moved);
                        queue.add(in.getAsset());
                    }
                }
            }
        }
    }

    Asset findSame(Long accountId, String ticker, String isin) {
        List<Asset> found = assets.findSame(accountId, ticker, isin);
        if (found.isEmpty()) return null;
        // an ISIN match wins over a ticker match
        return found.stream().filter(a -> isin != null && isin.equalsIgnoreCase(a.getIsin())).findFirst()
                .orElse(found.get(0));
    }

    LocalDate today() {
        return LocalDate.now(clock);
    }

    boolean isToday(LocalDate date) {
        return date == null || date.equals(today());
    }

    private AssetOperation operation(Long id) {
        return operations.findById(id).orElseThrow(() -> new NotFoundException("error.operationNotFound"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
