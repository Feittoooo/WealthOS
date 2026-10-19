// Plain functions so they can be unit tested without Angular.

type Num = number | null | undefined;

function isNumber(v: Num): v is number {
  return typeof v === 'number' && Number.isFinite(v);
}

function currencyFormat(locale: string, currency: string, min: number, max: number): Intl.NumberFormat {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: min,
    maximumFractionDigits: max,
    // es-ES skips the thousands dot on 4-digit numbers by default, which looks inconsistent
    useGrouping: 'always'
  } as unknown as Intl.NumberFormatOptions);
}

export function moneyText(value: Num, locale: string, currency: string, signed = false): string {
  if (!isNumber(value)) return '-';
  const text = currencyFormat(locale, currency, 2, 2).format(value);
  return signed && value > 0 ? `+${text}` : text;
}

export function priceText(value: Num, locale: string, currency: string): string {
  if (!isNumber(value)) return '-';
  return currencyFormat(locale, currency, 2, Math.abs(value) < 1 ? 6 : 4).format(value);
}

export function pctText(value: Num, locale: string, signed = false): string {
  if (!isNumber(value)) return '-';
  const text = new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value) + '%';
  return signed && value > 0 ? `+${text}` : text;
}

export function qtyText(value: Num, locale: string): string {
  if (!isNumber(value)) return '-';
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 8, useGrouping: 'always' } as unknown as Intl.NumberFormatOptions)
    .format(value);
}

// CSS class for green/red numbers
export function tone(value: Num): 'positive' | 'negative' | 'neutral' {
  if (!isNumber(value) || value === 0) return 'neutral';
  return value > 0 ? 'positive' : 'negative';
}
