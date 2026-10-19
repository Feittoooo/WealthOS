package com.wealthos.tax.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

// An invoice issued (ingreso) or paid (gasto). baseAmount is the base imponible, before IVA and IRPF.
@Entity
@Table(name = "tax_entries")
public class TaxEntry {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 160)
    private String concept;

    @Column(length = 120)
    private String counterparty;

    @Column(length = 60)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(10)")
    private TaxEntryType type;

    @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "iva_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaRate = BigDecimal.valueOf(21);

    // 7 or 15 usually. Only used on ingresos.
    @Column(name = "irpf_retention_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal irpfRetentionRate = BigDecimal.ZERO;

    // gastos only: counts for IRPF and its IVA can be deducted
    @Column(nullable = false)
    private boolean deductible = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public boolean isIncome() {
        return type == TaxEntryType.INGRESO;
    }

    public BigDecimal getIvaAmount() {
        return baseAmount.multiply(ivaRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRetentionAmount() {
        if (!isIncome()) return BigDecimal.ZERO;
        return baseAmount.multiply(irpfRetentionRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    // what actually hits the bank: base + IVA - withholding
    public BigDecimal getTotal() {
        return baseAmount.add(getIvaAmount()).subtract(getRetentionAmount());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }

    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public TaxEntryType getType() { return type; }
    public void setType(TaxEntryType type) { this.type = type; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getIvaRate() { return ivaRate; }
    public void setIvaRate(BigDecimal ivaRate) { this.ivaRate = ivaRate; }

    public BigDecimal getIrpfRetentionRate() { return irpfRetentionRate; }
    public void setIrpfRetentionRate(BigDecimal irpfRetentionRate) { this.irpfRetentionRate = irpfRetentionRate; }

    public boolean isDeductible() { return deductible; }
    public void setDeductible(boolean deductible) { this.deductible = deductible; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
