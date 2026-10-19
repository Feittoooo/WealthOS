package com.wealthos.portfolio.dto;

import com.wealthos.portfolio.domain.AssetLedger;
import com.wealthos.portfolio.domain.OperationType;

import java.math.BigDecimal;
import java.time.LocalDate;

// counterpart: ticker of the other fund in a transfer
public record OperationView(
        Long id,
        OperationType type,
        LocalDate date,
        BigDecimal units,
        BigDecimal price,
        BigDecimal amount,
        BigDecimal realisedGain,
        BigDecimal unitsAfter,
        String transferId,
        String counterpart,
        boolean cashCredited) {

    public static OperationView of(AssetLedger.Line line, String counterpart) {
        var op = line.op();
        return new OperationView(op.getId(), op.getType(), op.getDate(), op.getUnits(), op.getPrice(), op.getAmount(),
                line.realisedGain(), line.unitsAfter(), op.getTransferId(), counterpart, op.isCashCredited());
    }
}
