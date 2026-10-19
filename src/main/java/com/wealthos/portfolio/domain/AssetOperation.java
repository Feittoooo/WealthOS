package com.wealthos.portfolio.domain;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One line of an asset's history: a purchase, a sale or one leg of a fund transfer.
 * The asset's quantity, cost and gains are always worked out from these (see AssetLedger).
 */
@Entity
@Table(name = "asset_operations")
public class AssetOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private OperationType type;

    @Column(name = "op_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal units;

    // price of one unit on that day
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    // TRANSFER_IN only: purchase cost brought over from the origin fund
    @Column(name = "carried_cost", precision = 19, scale = 2)
    private BigDecimal carriedCost;

    @Column(name = "transfer_id", length = 36)
    private String transferId;

    // SELL only: whether the proceeds were added to the account's cash
    @Column(name = "cash_credited", nullable = false, columnDefinition = "boolean default false")
    private boolean cashCredited;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AssetOperation() {
    }

    public AssetOperation(OperationType type, LocalDate date, BigDecimal units, BigDecimal price) {
        this.type = type;
        this.date = date;
        this.units = units;
        this.price = price;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public BigDecimal getAmount() {
        return units.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }

    public Asset getAsset() { return asset; }
    void setAsset(Asset asset) { this.asset = asset; }

    public OperationType getType() { return type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getUnits() { return units; }
    public void setUnits(BigDecimal units) { this.units = units; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCarriedCost() { return carriedCost; }
    public void setCarriedCost(BigDecimal carriedCost) { this.carriedCost = carriedCost; }

    public String getTransferId() { return transferId; }
    public void setTransferId(String transferId) { this.transferId = transferId; }

    public boolean isCashCredited() { return cashCredited; }
    public void setCashCredited(boolean cashCredited) { this.cashCredited = cashCredited; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
