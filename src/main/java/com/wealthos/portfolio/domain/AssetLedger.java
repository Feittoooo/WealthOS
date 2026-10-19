package com.wealthos.portfolio.domain;

import com.wealthos.common.error.BusinessException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Replays an asset's operations in date order (average-cost method) and answers: how many
 * units, what they cost, what was realised, and what you held on a given day.
 *
 * Nothing here touches the database, so it's cheap to call and easy to test. It refuses
 * histories where you'd hold fewer than zero units at some point (e.g. deleting the purchase
 * a later sale depended on).
 */
public final class AssetLedger {

    // anything closer than this to zero counts as zero
    public static final BigDecimal DUST = new BigDecimal("0.00000001");

    /** State right after one operation. */
    public record Line(AssetOperation op, BigDecimal unitsAfter, BigDecimal costAfter,
                       BigDecimal realisedGain, BigDecimal costMoved) {
    }

    private final List<Line> lines;

    private AssetLedger(List<Line> lines) {
        this.lines = lines;
    }

    public static AssetLedger of(List<AssetOperation> operations) {
        List<AssetOperation> ordered = new ArrayList<>(operations);
        // same day: in the order they were saved; ones not saved yet go last
        ordered.sort(Comparator.comparing(AssetOperation::getDate)
                .thenComparing(AssetOperation::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Line> lines = new ArrayList<>();
        BigDecimal units = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (AssetOperation op : ordered) {
            BigDecimal realised = null;
            BigDecimal moved = null;
            switch (op.getType()) {
                case BUY -> {
                    units = units.add(op.getUnits());
                    cost = cost.add(op.getUnits().multiply(op.getPrice()));
                }
                case TRANSFER_IN -> {
                    units = units.add(op.getUnits());
                    cost = cost.add(op.getCarriedCost() != null ? op.getCarriedCost()
                            : op.getUnits().multiply(op.getPrice()));
                }
                case SELL, TRANSFER_OUT -> {
                    BigDecimal out = op.getUnits();
                    if (out.subtract(units).compareTo(DUST) > 0) {
                        throw new BusinessException("error.negativeHoldings", op.getDate().toString());
                    }
                    if (out.compareTo(units) > 0) out = units;
                    moved = units.signum() == 0 ? BigDecimal.ZERO
                            : cost.multiply(out).divide(units, MathContext.DECIMAL64);
                    if (op.getType() == OperationType.SELL) {
                        realised = op.getUnits().multiply(op.getPrice()).subtract(moved).setScale(2, RoundingMode.HALF_UP);
                    }
                    units = units.subtract(out);
                    cost = cost.subtract(moved);
                    if (units.compareTo(DUST) <= 0) {
                        units = BigDecimal.ZERO;
                        cost = BigDecimal.ZERO;
                    }
                }
            }
            lines.add(new Line(op, units, cost, realised, moved == null ? null : moved.setScale(2, RoundingMode.HALF_UP)));
        }
        return new AssetLedger(lines);
    }

    public List<Line> lines() {
        return lines;
    }

    public BigDecimal units() {
        return lines.isEmpty() ? BigDecimal.ZERO : lines.get(lines.size() - 1).unitsAfter();
    }

    public BigDecimal cost() {
        return lines.isEmpty() ? BigDecimal.ZERO : lines.get(lines.size() - 1).costAfter().setScale(2, RoundingMode.HALF_UP);
    }

    // from the unrounded cost: rounding it to cents first moves the average after a partial sale
    public BigDecimal averagePrice() {
        BigDecimal u = units();
        if (u.signum() == 0) return BigDecimal.ZERO;
        return lines.get(lines.size() - 1).costAfter().divide(u, 6, RoundingMode.HALF_UP);
    }

    public BigDecimal realisedGain() {
        return lines.stream().map(Line::realisedGain).filter(g -> g != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public LocalDate firstDate() {
        return lines.isEmpty() ? null : lines.get(0).op().getDate();
    }

    public LocalDate lastDate() {
        return lines.isEmpty() ? null : lines.get(lines.size() - 1).op().getDate();
    }

    public boolean isOpen() {
        return units().signum() > 0;
    }

    /** Units held at the end of that day. */
    public BigDecimal unitsAt(LocalDate day) {
        BigDecimal result = BigDecimal.ZERO;
        for (Line l : lines) {
            if (l.op().getDate().isAfter(day)) break;
            result = l.unitsAfter();
        }
        return result;
    }

    /** Purchase cost of what was held at the end of that day. */
    public BigDecimal costAt(LocalDate day) {
        BigDecimal result = BigDecimal.ZERO;
        for (Line l : lines) {
            if (l.op().getDate().isAfter(day)) break;
            result = l.costAfter();
        }
        return result;
    }

    // the carried cost of a TRANSFER_OUT, needed to keep the other leg in sync after edits
    public BigDecimal costMovedBy(AssetOperation op) {
        return lines.stream().filter(l -> l.op() == op).map(Line::costMoved).findFirst().orElse(null);
    }
}
