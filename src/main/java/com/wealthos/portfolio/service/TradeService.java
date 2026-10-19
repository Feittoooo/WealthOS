package com.wealthos.portfolio.service;

import com.wealthos.activity.ActivityLog;
import com.wealthos.activity.TransactionType;
import com.wealthos.common.error.BusinessException;
import com.wealthos.common.format.NumberFormats;
import com.wealthos.portfolio.domain.Account;
import com.wealthos.portfolio.domain.Asset;
import com.wealthos.portfolio.domain.AssetLedger;
import com.wealthos.portfolio.domain.AssetOperation;
import com.wealthos.portfolio.domain.AssetType;
import com.wealthos.portfolio.domain.OperationType;
import com.wealthos.portfolio.dto.AmountMode;
import com.wealthos.portfolio.dto.SellRequest;
import com.wealthos.portfolio.dto.SellResult;
import com.wealthos.portfolio.dto.TransferRequest;
import com.wealthos.portfolio.dto.TransferResult;
import com.wealthos.portfolio.price.PriceService;
import com.wealthos.portfolio.repository.AssetOperationRepository;
import com.wealthos.portfolio.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Sales and fund transfers, both added as operations to the asset (average-cost method).
 * Prices are optional: without a date we use today, without a price the price on that date
 * (for today, the price we already have stored).
 */
@Service
@Transactional
public class TradeService {

    private final AssetRepository assets;
    private final AssetOperationRepository operations;
    private final AssetService assetService;
    private final AccountService accounts;
    private final PriceService prices;
    private final ActivityLog activity;
    private final NumberFormats formats;

    public TradeService(AssetRepository assets, AssetOperationRepository operations, AssetService assetService,
                        AccountService accounts, PriceService prices, ActivityLog activity, NumberFormats formats) {
        this.assets = assets;
        this.operations = operations;
        this.assetService = assetService;
        this.accounts = accounts;
        this.prices = prices;
        this.activity = activity;
        this.formats = formats;
    }

    public SellResult sell(Long assetId, SellRequest r) {
        Asset asset = assetService.get(assetId);
        LocalDate date = date(r.date(), asset);
        BigDecimal price = r.price() != null ? r.price() : priceOn(asset, date);
        BigDecimal held = asset.ledger().unitsAt(date);
        BigDecimal units = units(r.mode(), r.amount(), held, price, "error.sellTooMuch", asset);

        AssetOperation op = new AssetOperation(OperationType.SELL, date, units, price);
        op.setCashCredited(r.creditCash());
        asset.addOperation(op);
        AssetLedger ledger = asset.ledger();
        BigDecimal realised = ledger.lines().stream().filter(l -> l.op() == op).findFirst()
                .map(AssetLedger.Line::realisedGain).orElse(BigDecimal.ZERO);

        Account account = asset.getAccount();
        BigDecimal proceeds = op.getAmount();
        // cash and assets are separate unless the user asks otherwise
        if (r.creditCash()) account.setBalance(account.getBalance().add(proceeds));
        if (assetService.isToday(date) && !asset.isAutoPrice()) {
            asset.setCurrentPrice(price);
            asset.setPriceUpdatedAt(LocalDateTime.now());
        }
        operations.save(op);
        assetService.syncTransfers(asset);

        activity.record(date, TransactionType.SELL, proceeds, account.getCurrency(), account.getName(),
                "activity.sold", formats.qty(units), asset.getTicker(),
                formats.price(price, account.getCurrency()), formats.signedMoney(realised, account.getCurrency()));
        return new SellResult(asset.getTicker(), date, units, price, proceeds, realised, !ledger.isOpen(), r.creditCash());
    }

    /**
     * Traspaso between funds. Nothing is sold, so nothing is realised: the purchase cost of the
     * units that leave the origin moves with them (that's how Hacienda treats it, the tax is
     * deferred). The number of units received comes from the prices on the transfer date.
     * A new destination fund that the account already holds is added to, like a purchase.
     */
    public TransferResult transfer(Long originId, TransferRequest r) {
        Asset origin = assetService.get(originId);
        if (origin.getAssetType() != AssetType.FUND) throw new BusinessException("error.transferFundsOnly");
        LocalDate date = date(r.date(), origin);

        Asset destination = destination(origin, r);
        BigDecimal originPrice = r.originPrice() != null ? r.originPrice() : priceOn(origin, date);
        BigDecimal held = origin.ledger().unitsAt(date);
        BigDecimal unitsOut = units(r.mode(), r.amount(), held, originPrice, "error.transferTooMuch", origin);

        String transferId = UUID.randomUUID().toString();
        AssetOperation out = new AssetOperation(OperationType.TRANSFER_OUT, date, unitsOut, originPrice);
        out.setTransferId(transferId);
        origin.addOperation(out);
        AssetLedger originLedger = origin.ledger();
        BigDecimal costCarried = originLedger.costMovedBy(out);
        BigDecimal value = out.getAmount();

        BigDecimal destinationPrice = r.destinationPrice() != null ? r.destinationPrice()
                : destination.getId() != null ? priceOn(destination, date)
                : prices.on(destination.getLookupSymbol(), AssetType.FUND, destination.getCurrency(), date)
                    .orElseThrow(() -> priceMissing(destination.getTicker(), date));
        BigDecimal unitsIn = value.divide(destinationPrice, 8, RoundingMode.HALF_DOWN);
        if (unitsIn.signum() == 0) throw new BusinessException("error.transferTooSmall");

        AssetOperation in = new AssetOperation(OperationType.TRANSFER_IN, date, unitsIn, destinationPrice);
        in.setTransferId(transferId);
        in.setCarriedCost(costCarried);
        destination.addOperation(in);
        if (destination.getCurrentPrice() == null
                || (assetService.isToday(date) && !destination.isAutoPrice())) {
            destination.setCurrentPrice(destinationPrice);
            destination.setPriceUpdatedAt(LocalDateTime.now());
        }
        destination.ledger();
        operations.save(out);
        if (destination.getId() == null) assets.save(destination); // new fund: persists its operation too
        else operations.save(in);
        assetService.syncTransfers(origin);

        Account account = origin.getAccount();
        activity.record(date, TransactionType.TRANSFER, value, account.getCurrency(), account.getName(),
                "activity.transferred", formats.qty(unitsOut), origin.getTicker(), destination.getTicker(),
                formats.money(value, account.getCurrency()));
        return new TransferResult(origin.getTicker(), destination.getTicker(), destination.getId(), date,
                unitsOut, value, unitsIn, costCarried, !originLedger.isOpen());
    }

    private Asset destination(Asset origin, TransferRequest r) {
        if (r.destinationAssetId() != null) {
            Asset d = assetService.get(r.destinationAssetId());
            if (d.getId().equals(origin.getId())) throw new BusinessException("error.transferSameFund");
            if (d.getAssetType() != AssetType.FUND) throw new BusinessException("error.transferFundsOnly");
            return d;
        }
        TransferRequest.NewFund f = r.newFund();
        if (f == null) throw new BusinessException("error.transferDestinationRequired");
        Account account = accounts.get(f.accountId());
        String ticker = f.ticker().trim().toUpperCase(Locale.ROOT);
        String isin = f.isin() == null || f.isin().isBlank() ? null : f.isin().trim().toUpperCase(Locale.ROOT);
        Asset same = assetService.findSame(account.getId(), ticker, isin);
        if (same != null) {
            if (same.getId().equals(origin.getId())) throw new BusinessException("error.transferSameFund");
            return same;
        }
        Asset d = new Asset();
        d.setAccount(account);
        d.setTicker(ticker);
        d.setIsin(isin);
        d.setName(f.name() == null || f.name().isBlank() ? ticker : f.name().trim());
        d.setAssetType(AssetType.FUND);
        d.setAutoPrice(f.autoPrice());
        d.setPriceSymbol(f.priceSymbol() == null || f.priceSymbol().isBlank() ? null : f.priceSymbol().trim());
        return d;
    }

    private LocalDate date(LocalDate date, Asset asset) {
        LocalDate today = assetService.today();
        LocalDate d = date != null ? date : today;
        if (d.isAfter(today)) throw new BusinessException("val.date.future");
        LocalDate first = asset.ledger().firstDate();
        if (first != null && d.isBefore(first)) {
            throw new BusinessException("error.dateBeforePurchase", asset.getTicker(), first.toString());
        }
        return d;
    }

    private BigDecimal priceOn(Asset asset, LocalDate date) {
        return prices.on(asset, date).orElseThrow(() -> priceMissing(asset.getTicker(), date));
    }

    private BusinessException priceMissing(String ticker, LocalDate date) {
        return new BusinessException(prices.isAvailable() ? "error.priceNotFoundOn" : "error.priceOnDateRequired",
                ticker, date.toString());
    }

    private BigDecimal units(AmountMode mode, BigDecimal amount, BigDecimal held, BigDecimal price,
                             String tooMuchCode, Asset asset) {
        if (mode == AmountMode.ALL) {
            if (held.signum() == 0) throw new BusinessException(tooMuchCode, formats.qty(held), asset.getTicker());
            return held;
        }
        if (amount == null) throw new BusinessException("error.amountRequired");
        BigDecimal units = mode == AmountMode.MONEY ? amount.divide(price, 8, RoundingMode.HALF_DOWN) : amount;
        BigDecimal excess = units.subtract(held);
        if (excess.signum() > 0) {
            if (excess.compareTo(AssetLedger.DUST) <= 0) return held;
            throw new BusinessException(tooMuchCode, formats.qty(held), asset.getTicker());
        }
        return units;
    }
}
