package com.wealthos.portfolio.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Something you hold in one account (an S&P 500 fund at your broker, some bitcoin in a wallet).
 * Every time you buy more it gets another operation instead of a new row. Quantity, cost and
 * gains are not stored: they come from the operations.
 */
@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 20)
    private String ticker;

    // used to recognise the same asset when you buy more, and later to ask for prices
    @Column(length = 12)
    private String isin;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, columnDefinition = "varchar(20)")
    private AssetType assetType;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal currentPrice;

    @Column(name = "price_updated_at")
    private LocalDateTime priceUpdatedAt;

    // the "lock": true = the current price comes from the price source and is refreshed every day
    @Column(name = "auto_price", nullable = false, columnDefinition = "boolean default false")
    private boolean autoPrice;

    // what to ask the price source for when it isn't the ticker or the ISIN
    @Column(name = "price_symbol", length = 40)
    private String priceSymbol;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC, id ASC")
    private List<AssetOperation> operations = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (priceUpdatedAt == null) priceUpdatedAt = createdAt;
    }

    public void addOperation(AssetOperation op) {
        op.setAsset(this);
        operations.add(op);
    }

    public void removeOperation(AssetOperation op) {
        operations.remove(op);
    }

    public AssetLedger ledger() {
        return AssetLedger.of(operations);
    }

    public String getCurrency() {
        return account != null ? account.getCurrency() : "EUR";
    }

    // what the price source is asked for: explicit symbol, then ISIN, then ticker
    public String getLookupSymbol() {
        if (priceSymbol != null && !priceSymbol.isBlank()) return priceSymbol;
        if (isin != null && !isin.isBlank()) return isin;
        return ticker;
    }

    public Long getId() { return id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public LocalDateTime getPriceUpdatedAt() { return priceUpdatedAt; }
    public void setPriceUpdatedAt(LocalDateTime priceUpdatedAt) { this.priceUpdatedAt = priceUpdatedAt; }

    public boolean isAutoPrice() { return autoPrice; }
    public void setAutoPrice(boolean autoPrice) { this.autoPrice = autoPrice; }

    public String getPriceSymbol() { return priceSymbol; }
    public void setPriceSymbol(String priceSymbol) { this.priceSymbol = priceSymbol; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<AssetOperation> getOperations() { return operations; }
}
