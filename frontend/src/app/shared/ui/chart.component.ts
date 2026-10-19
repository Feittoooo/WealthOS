import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, afterRenderEffect, inject, input, viewChild
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { FormatService } from '../../core/format/format.service';
import { ThemeService } from '../../core/ui-state/theme.service';

Chart.register(...registerables);

/**
 * Line or doughnut chart. Rebuilt when the data or the theme changes (colours come from CSS).
 * A line chart can carry a second, dashed series (e.g. money invested under the value).
 */
@Component({
  selector: 'app-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'chart' },
  template: `<canvas #canvas [attr.aria-label]="label()" role="img"></canvas>`
})
export class ChartComponent implements OnDestroy {
  readonly kind = input.required<'line' | 'doughnut'>();
  readonly labels = input.required<string[]>();
  readonly values = input.required<number[]>();
  readonly colors = input<string[]>([]);
  readonly label = input('');
  readonly secondary = input<number[] | null>(null);
  readonly secondaryLabel = input('');

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly format = inject(FormatService);
  private readonly theme = inject(ThemeService);
  private chart: Chart | null = null;

  constructor() {
    afterRenderEffect(() => {
      this.theme.theme(); // rebuild on theme change
      this.secondary();
      this.render(this.kind(), this.labels(), this.values(), this.colors());
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private render(kind: 'line' | 'doughnut', labels: string[], values: number[], colors: string[]): void {
    this.chart?.destroy();
    const css = getComputedStyle(document.documentElement);
    const v = (name: string) => css.getPropertyValue(name).trim();
    const money = (n: number) => this.format.money(n);
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    Chart.defaults.font.size = 11;
    Chart.defaults.color = v('--text-muted');

    const tooltip = {
      backgroundColor: v('--surface-raised'),
      borderColor: v('--border-strong'),
      borderWidth: 1,
      titleColor: v('--text-muted'),
      bodyColor: v('--text'),
      padding: 8,
      cornerRadius: 3,
      displayColors: kind === 'doughnut'
    };

    const canvas = this.canvas().nativeElement;
    if (kind === 'line') {
      const second = this.secondary();
      const config: ChartConfiguration<'line'> = {
        type: 'line',
        data: {
          labels,
          datasets: [{
            label: this.label(), data: values, borderColor: v('--accent'), backgroundColor: v('--accent-faint'),
            fill: true, borderWidth: 1.5, tension: 0.2, pointRadius: values.length === 1 ? 3 : 0, pointHoverRadius: 3
          }, ...(second ? [{
            label: this.secondaryLabel(), data: second, borderColor: v('--text-muted'), borderDash: [4, 3],
            fill: false, borderWidth: 1, tension: 0.2, pointRadius: 0, pointHoverRadius: 3
          }] : [])]
        },
        options: {
          responsive: true, maintainAspectRatio: false, animation: false,
          interaction: { mode: 'index', intersect: false },
          plugins: {
            legend: { display: false },
            tooltip: {
              ...tooltip,
              callbacks: { label: c => second ? `${c.dataset.label}: ${money(c.parsed.y ?? 0)}` : money(c.parsed.y ?? 0) }
            }
          },
          scales: {
            x: { grid: { display: false }, border: { color: v('--border') }, ticks: { maxTicksLimit: 8, maxRotation: 0 } },
            y: {
              grid: { color: v('--border') }, border: { display: false }, grace: '5%',
              ticks: { maxTicksLimit: 5, callback: value => this.format.compactMoney(Number(value)) }
            }
          }
        }
      };
      this.chart = new Chart(canvas, config);
      return;
    }
    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{ data: values, backgroundColor: colors, borderColor: v('--surface'), borderWidth: 2 }]
      },
      options: {
        responsive: true, maintainAspectRatio: false, animation: false, cutout: '68%',
        plugins: { legend: { display: false }, tooltip: { ...tooltip, callbacks: { label: c => ` ${money(c.parsed)}` } } }
      }
    };
    this.chart = new Chart(canvas, config);
  }
}
