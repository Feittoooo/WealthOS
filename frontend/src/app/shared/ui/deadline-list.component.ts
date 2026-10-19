import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { NgClass } from '@angular/common';
import { FiscalDeadline } from '../../core/api/models';
import { FormatService } from '../../core/format/format.service';
import { MoneyPipe } from '../../core/format/format.pipes';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

// Used on the dashboard and on the tax page.
@Component({
  selector: 'app-deadline-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, TranslatePipe, MoneyPipe],
  template: `
    <table class="table table--plain">
      <tbody>
        @for (d of deadlines(); track d.date + d.titleKey) {
          <tr>
            <td class="nowrap" [ngClass]="{ 'text-negative': d.daysLeft <= 7 }">
              {{ fmt.date(d.date) }}<br>
              <small class="muted">{{ fmt.countdown(d.daysLeft) }}</small>
            </td>
            <td>
              {{ d.titleKey | t }} <span class="tag">{{ d.periodKey | t: d.periodArgs }}</span><br>
              <small class="muted">{{ d.descriptionKey | t }}</small>
            </td>
            <td class="num">
              @if (d.estimate !== null) {
                {{ d.estimate | money }}<br><small class="muted">{{ 'common.estimated' | t }}</small>
              }
            </td>
          </tr>
        } @empty {
          <tr><td class="muted">{{ 'deadline.none' | t }}</td></tr>
        }
      </tbody>
    </table>
  `
})
export class DeadlineListComponent {
  readonly deadlines = input.required<FiscalDeadline[]>();
  protected readonly fmt = inject(FormatService);
}
