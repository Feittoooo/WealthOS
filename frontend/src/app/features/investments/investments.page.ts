import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { NgClass } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import {
  AccountView, AssetDetail, AssetView, BuyResult, PortfolioOverview, SellResult, TransferResult
} from '../../core/api/models';
import { FORMAT_PIPES } from '../../core/format/format.pipes';
import { FormatService } from '../../core/format/format.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ConfirmService } from '../../core/ui-state/confirm.service';
import { DataEventsService } from '../../core/ui-state/data-events.service';
import { ToastService } from '../../core/ui-state/toast.service';
import { AllocationComponent } from '../../shared/ui/allocation.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { AccountsTableComponent } from './components/accounts-table.component';
import { AssetsTableComponent } from './components/assets-table.component';
import { AccountDialogComponent } from './dialogs/account-dialog.component';
import { AssetDetailDialogComponent } from './dialogs/asset-detail-dialog.component';
import { AssetEditDialogComponent } from './dialogs/asset-edit-dialog.component';
import { BuyDialogComponent } from './dialogs/buy-dialog.component';
import { SellDialogComponent } from './dialogs/sell-dialog.component';
import { TransferDialogComponent } from './dialogs/transfer-dialog.component';
import { InvestmentsApi } from './investments.api';

/**
 * Container: loads the overview, opens dialogs and reloads after any change. The asset detail
 * stays open behind the sell / transfer / buy dialogs and is refreshed when they finish.
 */
@Component({
  selector: 'app-investments-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, TranslatePipe, ...FORMAT_PIPES, AllocationComponent, EmptyStateComponent,
    AssetsTableComponent, AccountsTableComponent, AccountDialogComponent, BuyDialogComponent,
    AssetDetailDialogComponent, AssetEditDialogComponent, SellDialogComponent, TransferDialogComponent],
  templateUrl: './investments.page.html'
})
export class InvestmentsPage {
  private readonly api = inject(InvestmentsApi);
  private readonly i18n = inject(I18nService);
  private readonly toasts = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly events = inject(DataEventsService);
  private readonly fmt = inject(FormatService);

  protected readonly overview = signal<PortfolioOverview | null>(null);

  // dialog state: open flag + the item being edited (null = new)
  protected readonly accountDialog = signal<{ account: AccountView | null } | null>(null);
  // buyDialog.asset set = "buy more" of that asset
  protected readonly buyDialog = signal<{ asset: AssetView | null } | null>(null);
  protected readonly detail = signal<AssetDetail | null>(null);
  protected readonly editing = signal<AssetView | null>(null);
  protected readonly selling = signal<AssetView | null>(null);
  protected readonly transferring = signal<AssetView | null>(null);

  constructor() {
    effect(() => {
      this.events.version();
      untracked(() => void this.load());
    });
  }

  protected newPurchase(): void {
    if (!this.overview()?.accounts.length) {
      this.accountDialog.set({ account: null });
      return;
    }
    this.buyDialog.set({ asset: null });
  }

  protected async openAsset(asset: AssetView): Promise<void> {
    this.detail.set(await firstValueFrom(this.api.asset(asset.id)));
  }

  protected accountSaved(name: string): void {
    const editing = this.accountDialog()?.account;
    this.accountDialog.set(null);
    this.toasts.success(this.i18n.t(editing ? 'flash.accountUpdated' : 'flash.accountCreated', [name]));
    void this.load();
  }

  protected bought(result: BuyResult): void {
    this.buyDialog.set(null);
    this.toasts.success(this.i18n.t(result.merged ? 'flash.boughtMore' : 'flash.positionAdded',
      [result.asset.ticker, this.fmt.qty(result.asset.quantity)]));
    result.warnings.forEach(w => this.toasts.warning(this.i18n.t(w)));
    void this.reload(result.asset.id);
  }

  protected assetSaved(detail: AssetDetail): void {
    this.editing.set(null);
    this.toasts.success(this.i18n.t('flash.positionUpdated', [detail.asset.ticker]));
    void this.reload(detail.asset.id);
  }

  protected sold(result: SellResult): void {
    const id = this.selling()?.id;
    this.selling.set(null);
    this.toasts.success(this.i18n.t('flash.sold', [this.fmt.qty(result.quantity), result.ticker,
      this.fmt.money(result.realisedGain, undefined, true)]));
    void this.reload(id);
  }

  protected transferred(result: TransferResult): void {
    const id = this.transferring()?.id;
    this.transferring.set(null);
    this.toasts.success(this.i18n.t('flash.transferred', [result.originTicker, result.destinationTicker,
      this.fmt.money(result.value)]));
    void this.reload(id);
  }

  // an operation was edited or deleted from inside the detail
  protected operationsChanged(detail: AssetDetail | null): void {
    this.detail.set(detail);
    void this.load();
  }

  protected async deleteAccount(account: AccountView): Promise<void> {
    const ok = await this.confirm.ask({
      titleKey: 'common.deleteTitle', titleArgs: [account.name],
      messageKey: account.positionsCount ? 'inv.deleteAccountMsgPositions' : 'inv.deleteAccountMsg',
      messageArgs: [account.positionsCount], confirmKey: 'inv.deleteAccount', danger: true
    });
    if (!ok) return;
    await firstValueFrom(this.api.deleteAccount(account.id));
    this.toasts.success(this.i18n.t('flash.accountDeleted', [account.name]));
    void this.load();
  }

  protected async deleteAsset(asset: AssetView): Promise<void> {
    const ok = await this.confirm.ask({
      titleKey: 'common.deleteTitle', titleArgs: [asset.ticker],
      messageKey: 'asset.deleteMsg', messageArgs: [asset.operationsCount],
      confirmKey: 'asset.delete', danger: true
    });
    if (!ok) return;
    await firstValueFrom(this.api.deleteAsset(asset.id));
    this.detail.set(null);
    this.toasts.success(this.i18n.t('flash.positionRemoved', [asset.ticker]));
    void this.load();
  }

  /** Reloads the overview, and the open detail if there is one (it may be gone by now). */
  private async reload(assetId?: number): Promise<void> {
    await this.load();
    const open = this.detail();
    if (!open || (assetId !== undefined && open.asset.id !== assetId)) return;
    const stillThere = this.overview()?.assets.some(a => a.id === open.asset.id);
    this.detail.set(stillThere ? await firstValueFrom(this.api.asset(open.asset.id)) : null);
  }

  private async load(): Promise<void> {
    this.overview.set(await firstValueFrom(this.api.overview()));
  }
}
