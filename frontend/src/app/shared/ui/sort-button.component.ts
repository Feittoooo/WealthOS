import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { TableSort } from './table-sort';

/** Clickable column header: <th><app-sort-button [sort]="sort" key="ticker" labelKey="inv.col.asset" /></th> */
@Component({
  selector: 'app-sort-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    <button type="button" class="th-sort" (click)="sort().toggle(key())"
            [attr.aria-sort]="sort().key() === key() ? (sort().desc() ? 'descending' : 'ascending') : null">
      {{ label() || (labelKey() | t) }} <span class="th-sort__mark">{{ sort().mark(key()) }}</span>
    </button>
  `
})
export class SortButtonComponent {
  // any: one button works for every row type
  readonly sort = input.required<TableSort<any>>();
  readonly key = input.required<string>();
  readonly labelKey = input('');
  // for labels that aren't translated (IVA, IRPF)
  readonly label = input('');
}
