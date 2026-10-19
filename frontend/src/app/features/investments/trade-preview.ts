import { AmountMode } from '../../core/api/models';

// Live previews in the dialogs. The server does the real calculation; this only mirrors it
// so the user sees the numbers before saving. Returns null when there isn't enough to go on.

export interface PositionPreview {
  units: number;
  cost: number;
  value: number;
  gain: number;
}

export interface SellPreview {
  units: number;
  proceeds: number;
  realised: number;
  remaining: number;
  tooMuch: boolean;
}

export interface TransferPreview {
  units: number;
  value: number;
  costCarried: number;
  unitsIn: number | null;
  tooMuch: boolean;
}

const n = (v: number | null | undefined): v is number => typeof v === 'number' && Number.isFinite(v) && v > 0;

export function unitsFor(mode: AmountMode, amount: number | null, held: number, price: number | null): number | null {
  if (mode === 'ALL') return held;
  if (!n(amount)) return null;
  if (mode === 'MONEY') return n(price) ? amount / price : null;
  return amount;
}

/** avg or current may be missing: like the server, one stands in for the other. */
export function positionPreview(mode: AmountMode, amount: number | null, avg: number | null,
                                current: number | null): PositionPreview | null {
  const buy = n(avg) ? avg : n(current) ? current : null;
  const now = n(current) ? current : buy;
  if (buy === null || now === null) return null;
  const units = unitsFor(mode, amount, 0, buy);
  if (units === null) return null;
  const cost = units * buy;
  const value = units * now;
  return { units, cost, value, gain: value - cost };
}

export function sellPreview(mode: AmountMode, amount: number | null, held: number, avg: number,
                            price: number | null): SellPreview | null {
  if (!n(price)) return null;
  const units = unitsFor(mode, amount, held, price);
  if (units === null) return null;
  return {
    units,
    proceeds: units * price,
    realised: (price - avg) * units,
    remaining: Math.max(held - units, 0),
    tooMuch: units - held > 1e-8
  };
}

export function transferPreview(mode: AmountMode, amount: number | null, held: number, avg: number,
                                originPrice: number | null, destinationPrice: number | null): TransferPreview | null {
  if (!n(originPrice)) return null;
  const units = unitsFor(mode, amount, held, originPrice);
  if (units === null) return null;
  const value = units * originPrice;
  return {
    units,
    value,
    costCarried: units * avg,
    unitsIn: n(destinationPrice) ? value / destinationPrice : null,
    tooMuch: units - held > 1e-8
  };
}
