import { Pipe, PipeTransform, inject } from '@angular/core';
import { FormatService } from './format.service';
import { tone } from './numbers';

// Impure where the output depends on the UI language (dates); pure otherwise.

@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly fmt = inject(FormatService);
  transform(value: number | null | undefined, currency?: string, signed = false): string {
    return this.fmt.money(value, currency, signed);
  }
}

@Pipe({ name: 'price' })
export class PricePipe implements PipeTransform {
  private readonly fmt = inject(FormatService);
  transform(value: number | null | undefined, currency?: string): string {
    return this.fmt.price(value, currency);
  }
}

@Pipe({ name: 'pct' })
export class PctPipe implements PipeTransform {
  private readonly fmt = inject(FormatService);
  transform(value: number | null | undefined, signed = false): string {
    return this.fmt.pct(value, signed);
  }
}

@Pipe({ name: 'qty' })
export class QtyPipe implements PipeTransform {
  private readonly fmt = inject(FormatService);
  transform(value: number | null | undefined): string {
    return this.fmt.qty(value);
  }
}

@Pipe({ name: 'localDate', pure: false })
export class LocalDatePipe implements PipeTransform {
  private readonly fmt = inject(FormatService);
  transform(value: string | Date | null | undefined, style: 'short' | 'medium' | 'long' = 'medium'): string {
    return this.fmt.date(value, style);
  }
}

@Pipe({ name: 'relativeTime', pure: false })
export class RelativeTimePipe implements PipeTransform {
  private readonly fmt = inject(FormatService);
  transform(value: string | null | undefined): string {
    return this.fmt.relative(value);
  }
}

@Pipe({ name: 'tone' })
export class TonePipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return tone(value);
  }
}

export const FORMAT_PIPES = [MoneyPipe, PricePipe, PctPipe, QtyPipe, LocalDatePipe, RelativeTimePipe, TonePipe] as const;
