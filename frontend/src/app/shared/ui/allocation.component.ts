import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { Slice } from '../../core/api/models';
import { MoneyPipe, PctPipe } from '../../core/format/format.pipes';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ChartComponent } from './chart.component';

/** Doughnut + legend with percentages. Slices with a key get translated (asset types, cash). */
@Component({
  selector: 'app-allocation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChartComponent, TranslatePipe, MoneyPipe, PctPipe],
  template: `
    @if (slices().length) {
      <div class="allocation">
        <app-chart kind="doughnut" class="chart--donut" [labels]="labels()" [values]="values()" [colors]="colors()" />
        <table class="table table--plain allocation__legend">
          <tbody>
            @for (s of slices(); track $index) {
              <tr>
                <td><span class="swatch" [style.background]="s.color"></span>{{ labels()[$index] }}</td>
                <td class="num">{{ share($index) | pct }}</td>
                <td class="num muted">{{ s.value | money }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else {
      <p class="muted">{{ emptyKey() | t }}</p>
    }
  `
})
export class AllocationComponent {
  readonly slices = input.required<Slice[]>();
  readonly emptyKey = input('dash.nothingToShow');
  private readonly i18n = inject(I18nService);

  protected readonly labels = computed(() => {
    this.i18n.language();
    return this.slices().map(s => (s.key ? this.i18n.t(s.key) : s.label ?? ''));
  });
  protected readonly values = computed(() => this.slices().map(s => s.value));
  protected readonly colors = computed(() => this.slices().map(s => s.color));
  private readonly total = computed(() => this.values().reduce((a, b) => a + b, 0));

  protected share(i: number): number {
    return this.total() ? (this.values()[i] / this.total()) * 100 : 0;
  }
}
