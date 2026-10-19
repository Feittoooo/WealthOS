import { describe, expect, it } from 'vitest';
import { AssetView } from '../../core/api/models';
import { findSameAsset, isIsin } from './asset-match';

const asset = (id: number, accountId: number, ticker: string, isin: string | null) =>
  ({ id, accountId, ticker, isin }) as AssetView;

const held = [asset(1, 10, 'WRLD', 'ZZ00LUMEN012'), asset(2, 10, 'ACME', null), asset(3, 20, 'WRLD', 'ZZ00LUMEN012')];

describe('findSameAsset', () => {
  it('matches by ISIN even when the ticker is different', () => {
    expect(findSameAsset(held, 10, 'SP500', 'zz00lumen012')?.id).toBe(1);
  });

  it('falls back to the ticker, ignoring case', () => {
    expect(findSameAsset(held, 10, ' acme ', '')?.id).toBe(2);
  });

  it('only looks inside the chosen account', () => {
    expect(findSameAsset(held, 20, 'WRLD', '')?.id).toBe(3);
    expect(findSameAsset(held, 30, 'WRLD', 'ZZ00LUMEN012')).toBeNull();
  });

  it('ignores an ISIN that is still being typed', () => {
    expect(findSameAsset(held, 10, 'NEW', 'ZZ00LUM')).toBeNull();
  });

  it('needs an account', () => {
    expect(findSameAsset(held, null, 'ACME', '')).toBeNull();
  });
});

describe('isIsin', () => {
  it('checks the shape, not the check digit', () => {
    expect(isIsin('ZZ00LUMEN012')).toBe(true);
    expect(isIsin('ZZ00LUMEN01X')).toBe(false);
    expect(isIsin('')).toBe(false);
  });
});
