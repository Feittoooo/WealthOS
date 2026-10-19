import { AssetView } from '../../core/api/models';

const ISIN = /^[A-Za-z]{2}[A-Za-z0-9]{9}[0-9]$/;

export function isIsin(value: string | null | undefined): boolean {
  return ISIN.test((value ?? '').trim());
}

/**
 * The asset a purchase will be added to, if any. Same rule as AssetService on the server:
 * same account, and the same ISIN or (failing that) the same ticker.
 */
export function findSameAsset(assets: readonly AssetView[], accountId: number | null, ticker: string,
                              isin: string): AssetView | null {
  if (!accountId) return null;
  const t = ticker.trim().toUpperCase();
  const i = isIsin(isin) ? isin.trim().toUpperCase() : '';
  const inAccount = assets.filter(a => a.accountId === accountId);
  return (i && inAccount.find(a => a.isin === i)) || (t && inAccount.find(a => a.ticker === t)) || null;
}
