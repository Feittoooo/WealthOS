import { describe, expect, it } from 'vitest';
import { moneyText, pctText, qtyText, tone } from './numbers';

// Intl output uses non-breaking spaces before the currency sign
const clean = (s: string) => s.replace(/\s/g, ' ');

describe('number formatting', () => {
  it('formats money the Spanish way with grouping on 4-digit amounts', () => {
    expect(clean(moneyText(1234.5, 'es-ES', 'EUR'))).toBe('1.234,50 €');
    expect(clean(moneyText(-7, 'es-ES', 'EUR'))).toBe('-7,00 €');
  });

  it('adds a plus sign only when asked and only for gains', () => {
    expect(clean(moneyText(10, 'es-ES', 'EUR', true))).toBe('+10,00 €');
    expect(clean(moneyText(-10, 'es-ES', 'EUR', true))).toBe('-10,00 €');
  });

  it('shows a dash for missing values', () => {
    expect(moneyText(null, 'es-ES', 'EUR')).toBe('-');
    expect(pctText(undefined, 'es-ES')).toBe('-');
  });

  it('keeps crypto decimals in quantities', () => {
    expect(qtyText(0.18, 'es-ES')).toBe('0,18');
    expect(qtyText(85.432, 'en-GB')).toBe('85.432');
  });

  it('picks a tone', () => {
    expect(tone(3)).toBe('positive');
    expect(tone(-1)).toBe('negative');
    expect(tone(0)).toBe('neutral');
  });
});
