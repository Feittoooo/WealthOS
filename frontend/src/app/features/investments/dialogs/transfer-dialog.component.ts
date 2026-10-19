import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AccountView, AmountMode, ApiError, AssetView, TransferRequest, TransferResult } from '../../../core/api/models';
import { MoneyPipe, PricePipe, QtyPipe } from '../../../core/format/format.pipes';
import { todayIso } from '../../../core/format/format.service';
import { toApiError } from '../../../core/http/api-error';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { LockToggleComponent } from '../../../shared/ui/lock-toggle.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { SegmentedComponent } from '../../../shared/ui/segmented.component';
import { InvestmentsApi } from '../investments.api';
import { transferPreview } from '../trade-preview';

type Destination = 'EXISTING' | 'NEW';

/**
 * Traspaso from one fund to another (existing or new). Not a sale: no gain is realised and the
 * purchase cost travels with the units. Prices and date are optional, like in a sale.
 */
@Component({
  selector: 'app-transfer-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, MoneyPipe, PricePipe, QtyPipe, ModalComponent, SegmentedComponent,
    LockToggleComponent, FormErrorComponent],
  templateUrl: './transfer-dialog.component.html'
})
export class TransferDialogComponent {
  readonly origin = input<AssetView | null>(null);
  readonly assets = input.required<AssetView[]>();
  readonly accounts = input.required<AccountView[]>();
  readonly closed = output<void>();
  readonly transferred = output<TransferResult>();

  private readonly api = inject(InvestmentsApi);
  protected readonly today = todayIso();
  protected readonly modeOptions = [
    { value: 'ALL' as AmountMode, labelKey: 'amount.all' },
    { value: 'UNITS' as AmountMode, labelKey: 'amount.units' },
    { value: 'MONEY' as AmountMode, labelKey: 'amount.money' }
  ];
  protected readonly destinationOptions = [
    { value: 'EXISTING' as Destination, labelKey: 'transfer.existingFund' },
    { value: 'NEW' as Destination, labelKey: 'transfer.newFund' }
  ];
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly otherFunds = computed(() =>
    this.assets().filter(p => p.open && p.assetType === 'FUND' && p.id !== this.origin()?.id));

  protected readonly form = inject(FormBuilder).group({
    mode: ['ALL' as AmountMode],
    amount: [null as number | null],
    date: [null as string | null],
    originPrice: [null as number | null],
    destination: ['EXISTING' as Destination],
    destinationAssetId: [null as number | null],
    newAccountId: [null as number | null],
    newTicker: [''],
    newIsin: [''],
    newName: [''],
    newAutoPrice: [false],
    destinationPrice: [null as number | null]
  });

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  private readonly isToday = computed(() => !this.values().date || this.values().date === this.today);

  protected readonly preview = computed(() => {
    const o = this.origin();
    if (!o) return null;
    const v = this.values();
    const originPrice = v.originPrice || (this.isToday() ? o.currentPrice : null);
    const existing = this.otherFunds().find(p => p.id === v.destinationAssetId);
    const destinationPrice = v.destinationPrice
      || (v.destination === 'EXISTING' && existing && this.isToday() ? existing.currentPrice : null);
    return transferPreview(v.mode ?? 'ALL', v.amount ?? null, o.quantity, o.avgBuyPrice, originPrice, destinationPrice);
  });

  constructor() {
    effect(() => {
      const o = this.origin();
      if (!o) return;
      this.error.set(null);
      const firstOther = this.otherFunds()[0];
      this.form.reset({
        mode: 'ALL', amount: null, date: null, originPrice: null,
        destination: firstOther ? 'EXISTING' : 'NEW',
        destinationAssetId: firstOther?.id ?? null,
        newAccountId: o.accountId, newTicker: '', newIsin: '', newName: '', newAutoPrice: false, destinationPrice: null
      });
    });
  }

  protected async submit(): Promise<void> {
    const o = this.origin();
    if (!o) return;
    const v = this.form.getRawValue();
    const body: TransferRequest = {
      mode: v.mode!,
      amount: v.mode === 'ALL' ? null : v.amount ?? null,
      date: v.date || null,
      originPrice: v.originPrice ?? null,
      destinationAssetId: v.destination === 'EXISTING' ? v.destinationAssetId ?? null : null,
      newFund: v.destination === 'NEW'
        ? { accountId: v.newAccountId!, ticker: (v.newTicker ?? '').trim(),
            isin: v.newIsin?.trim().toUpperCase() || null, name: v.newName?.trim() || null,
            autoPrice: !!v.newAutoPrice, priceSymbol: null }
        : null,
      destinationPrice: v.destinationPrice ?? null
    };
    this.saving.set(true);
    this.error.set(null);
    try {
      this.transferred.emit(await firstValueFrom(this.api.transfer(o.id, body)));
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
