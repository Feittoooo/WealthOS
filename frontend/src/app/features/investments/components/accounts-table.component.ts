import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { NgClass } from '@angular/common';
import { AccountView } from '../../../core/api/models';
import { MoneyPipe, PctPipe, TonePipe } from '../../../core/format/format.pipes';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { IconComponent } from '../../../shared/ui/icon.component';
import { SortButtonComponent } from '../../../shared/ui/sort-button.component';
import { TableSort } from '../../../shared/ui/table-sort';

// Cash and positions in separate columns, total = both.
@Component({
  selector: 'app-accounts-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, TranslatePipe, MoneyPipe, PctPipe, TonePipe, IconComponent, SortButtonComponent],
  template: `
    <div class="table-scroll">
      <table class="table">
        <thead>
          <tr>
            <th><app-sort-button [sort]="sort" key="name" labelKey="form.account" /></th>
            <th class="num"><app-sort-button [sort]="sort" key="cashBalance" labelKey="acc.col.cash" /></th>
            <th class="num"><app-sort-button [sort]="sort" key="positionsValue" labelKey="acc.col.invested" /></th>
            <th class="num"><app-sort-button [sort]="sort" key="positionsGain" labelKey="inv.col.gain" /></th>
            <th class="num"><app-sort-button [sort]="sort" key="total" labelKey="common.total" /></th>
            <th class="num"><app-sort-button [sort]="sort" key="weight" labelKey="inv.col.weight" /></th>
            <th><span class="sr-only">{{ 'common.actions' | t }}</span></th>
          </tr>
        </thead>
        <tbody>
          @for (a of rows(); track a.id) {
            <tr>
              <td>
                <strong>{{ a.name }}</strong><br>
                <small class="muted">{{ 'accountType.' + a.type | t }}@if (a.institution) { · {{ a.institution }} } · {{ a.currency }}</small>
              </td>
              <td class="num">{{ a.cashBalance | money: a.currency }}</td>
              <td class="num">
                @if (a.positionsCount) {
                  {{ a.positionsValue | money: a.currency }}<br>
                  <small class="muted">{{ 'acc.positions' | t: [a.positionsCount] }}</small>
                } @else { <span class="muted">-</span> }
              </td>
              <td class="num" [ngClass]="a.positionsGain | tone">
                {{ a.positionsCount ? (a.positionsGain | money: a.currency : true) : '-' }}
              </td>
              <td class="num"><strong>{{ a.total | money: a.currency }}</strong></td>
              <td class="num">{{ a.weight | pct }}</td>
              <td class="actions">
                <button type="button" class="icon-button" (click)="edit.emit(a)" [title]="'common.edit' | t"><app-icon name="pencil" /></button>
                <button type="button" class="icon-button icon-button--danger" (click)="remove.emit(a)" [title]="'common.delete' | t"><app-icon name="trash" /></button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class AccountsTableComponent {
  readonly accounts = input.required<AccountView[]>();
  readonly edit = output<AccountView>();
  readonly remove = output<AccountView>();

  protected readonly sort = new TableSort<AccountView>('total');
  protected readonly rows = computed(() => this.sort.apply(this.accounts()));
}
