import { describe, expect, it } from 'vitest';
import { positionPreview, sellPreview, transferPreview, unitsFor } from './trade-preview';

describe('unitsFor', () => {
  it('handles the three modes', () => {
    expect(unitsFor('ALL', null, 12, 10)).toBe(12);
    expect(unitsFor('UNITS', 5, 12, 10)).toBe(5);
    expect(unitsFor('MONEY', 100, 12, 20)).toBe(5);
  });

  it('needs a price to turn money into units', () => {
    expect(unitsFor('MONEY', 100, 12, null)).toBeNull();
  });
});

describe('positionPreview', () => {
  it('uses both prices when there are two', () => {
    expect(positionPreview('UNITS', 10, 50, 60)).toEqual({ units: 10, cost: 500, value: 600, gain: 100 });
  });

  it('assumes cost = value when only one price is known', () => {
    expect(positionPreview('UNITS', 10, null, 60)).toEqual({ units: 10, cost: 600, value: 600, gain: 0 });
    expect(positionPreview('UNITS', 10, 50, null)).toEqual({ units: 10, cost: 500, value: 500, gain: 0 });
  });

  it('works out units from an amount of money', () => {
    expect(positionPreview('MONEY', 1000, 20, 25)?.units).toBe(50);
  });

  it('gives up without any price', () => {
    expect(positionPreview('UNITS', 10, null, null)).toBeNull();
  });
});

describe('sellPreview', () => {
  it('computes proceeds and realised gain at average cost', () => {
    expect(sellPreview('UNITS', 4, 10, 50, 80)).toEqual({ units: 4, proceeds: 320, realised: 120, remaining: 6, tooMuch: false });
  });

  it('flags selling more than held', () => {
    expect(sellPreview('UNITS', 11, 10, 50, 80)?.tooMuch).toBe(true);
  });

  it('sells everything with ALL', () => {
    expect(sellPreview('ALL', null, 10, 50, 40)?.realised).toBe(-100);
  });
});

describe('transferPreview', () => {
  it('carries the purchase cost and converts value into destination units', () => {
    const p = transferPreview('UNITS', 10, 20, 100, 150, 30);
    expect(p).toEqual({ units: 10, value: 1500, costCarried: 1000, unitsIn: 50, tooMuch: false });
  });

  it('leaves units in empty while the destination price is unknown', () => {
    expect(transferPreview('ALL', null, 20, 100, 150, null)?.unitsIn).toBeNull();
  });
});
