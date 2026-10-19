import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import {
  ASSET_TYPES, AccountView, AmountMode, ApiError, AssetType, AssetView, BuyRequest, BuyResult, PriceSourceStatus
} from '../../../core/api/models';
import { MoneyPipe, QtyPipe, TonePipe } from '../../../core/format/format.pipes';
import { todayIso } from '../../../core/format/format.service';
import { toApiError } from '../../../core/http/api-error';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { LockToggleComponent } from '../../../shared/ui/lock-toggle.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { SegmentedComponent } from '../../../shared/ui/segmented.component';
import { InvestmentsApi } from '../investments.api';
import { findSameAsset } from '../asset-match';
import { positionPreview } from '../trade-preview';

/**
 * A purchase. If the account already holds that asset (by ISIN or ticker) the units are added
 * to it, and the dialog says so before saving. Prices and date are optional, as on the server.
 * Opened from an asset ("buy more") the asset fields come filled in and locked.
 */
@Component({
  selector: 'app-buy-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, MoneyPipe, QtyPipe, TonePipe, ModalComponent, SegmentedComponent,
    LockToggleComponent, FormErrorComponent],
  templateUrl: './buy-dialog.component.html'
})
export class BuyDialogComponent {
  readonly open = input(false);
  // set when buying more of an asset already held
  readonly asset = input<AssetView | null>(null);
  readonly accounts = input.required<AccountView[]>();
  readonly assets = input.required<AssetView[]>();
  readonly priceSource = input.required<PriceSourceStatus>();
  readonly closed = output<void>();
  readonly saved = output<BuyResult>();

  private readonly api = inject(InvestmentsApi);
  protected readonly assetTypes = ASSET_TYPES;
  protected readonly today = todayIso();
  protected readonly modeOptions = [
    { value: 'UNITS' as AmountMode, labelKey: 'amount.units' },
    { value: 'MONEY' as AmountMode, labelKey: 'amount.money' }
  ];
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).group({
    accountId: [null as number | null, Validators.required],
    assetType: ['STOCK' as AssetType],
    ticker: ['', [Validators.required, Validators.maxLength(20)]],
    isin: ['', Validators.pattern(/^$|^[A-Za-z]{2}[A-Za-z0-9]{9}[0-9]$/)],
    name: [''],
    amountMode: ['UNITS' as AmountMode],
    amount: [null as number | null, [Validators.required, Validators.min(0.00000001)]],
    price: [null as number | null],
    currentPrice: [null as number | null],
    date: [null as string | null],
    autoPrice: [false]
  });

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.value });
  protected readonly locked = computed(() => !!this.values().autoPrice);

  // what the purchase will be added to, if anything
  protected readonly target = computed(() => {
    const v = this.values();
    return findSameAsset(this.assets(), v.accountId ?? null, v.ticker ?? '', v.isin ?? '');
  });

  protected readonly preview = computed(() => {
    const v = this.values();
    return positionPreview(v.amountMode ?? 'UNITS', v.amount ?? null, v.price ?? null,
      v.autoPrice ? null : v.currentPrice ?? null);
  });

  constructor() {
    effect(() => {
      if (!this.open()) return;
      this.error.set(null);
      const a = this.asset();
      this.form.reset({
        accountId: a?.accountId ?? this.accounts()[0]?.id ?? null,
        assetType: a?.assetType ?? 'STOCK',
        ticker: a?.ticker ?? '',
        isin: a?.isin ?? '',
        name: a?.name ?? '',
        amountMode: 'UNITS', amount: null, price: null,
        currentPrice: a && !a.autoPrice ? a.currentPrice : null,
        date: null,
        autoPrice: a?.autoPrice ?? false
      });
      const fixed = ['accountId', 'assetType', 'ticker', 'isin', 'name'] as const;
      fixed.forEach(k => a ? this.form.controls[k].disable() : this.form.controls[k].enable());
      this.syncLock(!!a?.autoPrice);
    });

    // with the lock on, the current price isn't typed
    this.form.controls.autoPrice.valueChanges.subscribe(on => this.syncLock(!!on));
  }

  private syncLock(on: boolean): void {
    const control = this.form.controls.currentPrice;
    if (on) control.disable({ emitEvent: false });
    else control.enable({ emitEvent: false });
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: BuyRequest = {
      accountId: v.accountId!,
      ticker: v.ticker!.trim(),
      isin: v.isin?.trim().toUpperCase() || null,
      name: v.name?.trim() || null,
      assetType: v.assetType!,
      amountMode: v.amountMode!,
      amount: v.amount!,
      price: v.price ?? null,
      currentPrice: v.autoPrice ? null : v.currentPrice ?? null,
      date: v.date || null,
      autoPrice: !!v.autoPrice
    };
    this.saving.set(true);
    this.error.set(null);
    try {
      this.saved.emit(await firstValueFrom(this.api.buy(body)));
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
