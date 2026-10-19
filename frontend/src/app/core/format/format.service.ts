import { Injectable, inject, signal } from '@angular/core';
import { I18nService } from '../i18n/i18n.service';
import { moneyText, pctText, qtyText, priceText } from './numbers';

/**
 * Numbers follow the configured number locale (es-ES by default, same as your bank), whatever
 * the UI language. Dates and relative times follow the UI language.
 */
@Injectable({ providedIn: 'root' })
export class FormatService {
  private readonly i18n = inject(I18nService);

  readonly numberLocale = signal('es-ES');
  readonly baseCurrency = signal('EUR');

  money(value: number | null | undefined, currency?: string, signed = false): string {
    return moneyText(value, this.numberLocale(), currency ?? this.baseCurrency(), signed);
  }

  // chart axes: "90 mil €", "90K €"
  compactMoney(value: number): string {
    return new Intl.NumberFormat(this.numberLocale(), {
      style: 'currency', currency: this.baseCurrency(), notation: 'compact', maximumFractionDigits: 1
    }).format(value);
  }

  price(value: number | null | undefined, currency?: string): string {
    return priceText(value, this.numberLocale(), currency ?? this.baseCurrency());
  }

  pct(value: number | null | undefined, signed = false): string {
    return pctText(value, this.numberLocale(), signed);
  }

  qty(value: number | null | undefined): string {
    return qtyText(value, this.numberLocale());
  }

  // style: short "19 oct", medium "19 oct 2026", long "lunes, 19 de octubre de 2026"
  date(value: string | Date | null | undefined, style: 'short' | 'medium' | 'long' = 'medium'): string {
    if (!value) return '';
    const d = typeof value === 'string' ? parseDate(value) : value;
    const options: Intl.DateTimeFormatOptions =
      style === 'short' ? { day: 'numeric', month: 'short' }
        : style === 'long' ? { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }
          : { day: 'numeric', month: 'short', year: 'numeric' };
    return new Intl.DateTimeFormat(this.i18n.dateLocale(), options).format(d).replace('.', '');
  }

  month(yearMonth: string, withYear = true): string {
    const [y, m] = yearMonth.split('-').map(Number);
    const options: Intl.DateTimeFormatOptions = withYear ? { month: 'short', year: '2-digit' } : { month: 'short' };
    return new Intl.DateTimeFormat(this.i18n.dateLocale(), options).format(new Date(y, m - 1, 1)).replace('.', '');
  }

  relative(value: string | null | undefined): string {
    if (!value) return this.i18n.t('time.never');
    const then = new Date(value);
    const now = new Date();
    const days = Math.floor((startOfDay(now) - startOfDay(then)) / 86_400_000);
    if (days <= 0) {
      const minutes = Math.floor((now.getTime() - then.getTime()) / 60_000);
      if (minutes < 1) return this.i18n.t('time.justNow');
      if (minutes < 60) return this.i18n.t('time.minutesAgo', [minutes]);
      return this.i18n.t('time.hoursAgo', [Math.floor(minutes / 60)]);
    }
    if (days === 1) return this.i18n.t('time.yesterday');
    if (days < 30) return this.i18n.t('time.daysAgo', [days]);
    if (days < 365) return this.i18n.t('time.monthsAgo', [Math.floor(days / 30)]);
    return this.i18n.t('time.yearsAgo', [Math.floor(days / 365)]);
  }

  countdown(daysLeft: number): string {
    if (daysLeft === 0) return this.i18n.t('time.today');
    if (daysLeft === 1) return this.i18n.t('time.tomorrow');
    return this.i18n.t('time.inDays', [daysLeft]);
  }
}

// "2026-10-19" as a local date (new Date("2026-10-19") would be UTC midnight)
export function parseDate(iso: string): Date {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function startOfDay(d: Date): number {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}
