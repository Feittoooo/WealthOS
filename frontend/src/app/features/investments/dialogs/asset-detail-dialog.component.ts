import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AssetDetail, AssetView, OperationView } from '../../../core/api/models';
import { FORMAT_PIPES } from '../../../core/format/format.pipes';
import { FormatService } from '../../../core/format/format.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { ConfirmService } from '../../../core/ui-state/confirm.service';
import { ToastService } from '../../../core/ui-state/toast.service';
import { IconComponent } from '../../../shared/ui/icon.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { SortButtonComponent } from '../../../shared/ui/sort-button.component';
import { TableSort } from '../../../shared/ui/table-sort';
import { InvestmentsApi } from '../investments.api';
import { OperationDialogComponent } from './operation-dialog.component';

/**
 * One asset opened up: the totals, the actions that work on the whole asset, and every
 * operation it's made of. Operations are edited and deleted here; the asset-wide actions
 * (buy more, sell, transfer, edit, delete) go back to the page, which owns those dialogs.
 */
@Component({
  selector: 'app-asset-detail-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, TranslatePipe, ...FORMAT_PIPES, ModalComponent, IconComponent, SortButtonComponent,
    OperationDialogComponent],
  templateUrl: './asset-detail-dialog.component.html'
})
export class AssetDetailDialogComponent {
  readonly detail = input<AssetDetail | null>(null);
  readonly closed = output<void>();
  readonly buyMore = output<AssetView>();
  readonly sell = output<AssetView>();
  readonly transfer = output<AssetView>();
  readonly edit = output<AssetView>();
  readonly remove = output<AssetView>();
  // an operation was changed or deleted; null when the asset went with it
  readonly changed = output<AssetDetail | null>();

  private readonly api = inject(InvestmentsApi);
  private readonly confirm = inject(ConfirmService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(I18nService);
  private readonly fmt = inject(FormatService);

  protected readonly sort = new TableSort<OperationView>('date');
  protected readonly editing = signal<OperationView | null>(null);
  protected readonly operations = computed(() => this.sort.apply(this.detail()?.operations ?? []));

  protected isTransfer(op: OperationView): boolean {
    return op.type === 'TRANSFER_IN' || op.type === 'TRANSFER_OUT';
  }

  protected editOperation(op: OperationView): void {
    if (this.isTransfer(op)) {
      this.toasts.warning(this.i18n.t('error.transferEdit'));
      return;
    }
    this.editing.set(op);
  }

  protected operationSaved(detail: AssetDetail): void {
    this.editing.set(null);
    this.toasts.success(this.i18n.t('flash.operationUpdated'));
    this.changed.emit(detail);
  }

  protected async deleteOperation(op: OperationView): Promise<void> {
    const asset = this.detail()?.asset;
    if (!asset) return;
    const last = asset.operationsCount === 1;
    const ok = await this.confirm.ask({
      titleKey: 'op.deleteTitle',
      titleArgs: [this.i18n.t('opType.' + op.type), this.fmt.date(op.date)],
      messageKey: this.isTransfer(op) ? 'op.deleteTransferMsg' : last ? 'op.deleteLastMsg' : 'op.deleteMsg',
      messageArgs: [op.counterpart ?? '', asset.ticker],
      confirmKey: 'common.delete', danger: true
    });
    if (!ok) return;
    const result = await firstValueFrom(this.api.deleteOperation(op.id));
    this.toasts.success(this.i18n.t('flash.operationDeleted'));
    this.changed.emit(result ?? null);
  }
}
