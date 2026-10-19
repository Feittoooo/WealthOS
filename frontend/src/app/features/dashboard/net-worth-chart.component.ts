import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { NetWorthRange, NetWorthSeries } from '../../core/api/models';
import { FormatService, todayIso } from '../../core/format/format.service';
import { toApiError } from '../../core/http/api-error';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ChartComponent } from '../../shared/ui/chart.component';
import { DashboardApi } from './dashboard.api';

const RANGES: NetWorthRange[] = ['ALL', '5Y', '3Y', '1Y', 'YTD', 'CUSTOM'];
const RANGE_KEY = 'wealthos.netWorthRange';

/**
 * Net worth over time, rebuilt by the server from every operation (so a purchase dated last
 * year shows up last year). The range is remembered per browser.
 */
@Component({
  selector: 'app-net-worth-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslatePipe, ChartComponent],
  templateUrl: './net-worth-chart.component.html'
})
export class NetWorthChartComponent {
  // bump to reload (the dashboard passes its data version)
  readonly version = input(0);

  private readonly api = inject(DashboardApi);
  private readonly fmt = inject(FormatService);
  private readonly i18n = inject(I18nService);

  protected readonly ranges = RANGES;
  protected readonly today = todayIso();
  protected readonly range = signal<NetWorthRange>(savedRange());
  protected readonly series = signal<NetWorthSeries | null>(null);
  protected readonly error = signal<string | null>(null);
  protected from = '';
  protected to = '';

  protected readonly labels = computed(() => {
    const s = this.series();
    if (!s) return [];
    return s.points.map(p => s.step === 'month' ? this.fmt.month(p.date.slice(0, 7)) : this.fmt.date(p.date, 'short'));
  });
  protected readonly values = computed(() => this.series()?.points.map(p => p.value) ?? []);
  protected readonly invested = computed(() => this.series()?.points.map(p => p.invested) ?? []);

  constructor() {
    effect(() => {
      this.version();
      // custom waits for the Apply button
      if (untracked(this.range) !== 'CUSTOM') untracked(() => void this.load());
    });
  }

  protected select(range: NetWorthRange): void {
    this.range.set(range);
    try {
      localStorage.setItem(RANGE_KEY, range);
    } catch {
      // private mode: just don't remember it
    }
    if (range === 'CUSTOM') {
      const s = this.series();
      this.from ||= s?.from ?? '';
      this.to ||= s?.to ?? this.today;
      return;
    }
    void this.load();
  }

  protected apply(): void {
    void this.load();
  }

  private async load(): Promise<void> {
    this.error.set(null);
    try {
      this.series.set(await firstValueFrom(this.api.netWorth(this.range(), this.from || null, this.to || null)));
    } catch (err) {
      const e = toApiError(err);
      this.error.set(e.message || this.i18n.t(e.code));
    }
  }
}

function savedRange(): NetWorthRange {
  try {
    const r = localStorage.getItem(RANGE_KEY) as NetWorthRange | null;
    // a custom range isn't stored, so start from a year in that case
    return r && RANGES.includes(r) && r !== 'CUSTOM' ? r : '1Y';
  } catch {
    return '1Y';
  }
}
