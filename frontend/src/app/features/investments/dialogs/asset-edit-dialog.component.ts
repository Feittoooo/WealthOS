import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import {
  ASSET_TYPES, ApiError, AssetDetail, AssetType, AssetUpdateRequest, AssetView, PriceSourceStatus
} from '../../../core/api/models';
import { toApiError } from '../../../core/http/api-error';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { LockToggleComponent } from '../../../shared/ui/lock-toggle.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { InvestmentsApi } from '../investments.api';

/**
 * What describes the asset as a whole: names, type and today's price. Units and buy prices
 * belong to the operations and are edited there.
 */
@Component({
  selector: 'app-asset-edit-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, ModalComponent, LockToggleComponent, FormErrorComponent],
  templateUrl: './asset-edit-dialog.component.html'
})
export class AssetEditDialogComponent {
  readonly asset = input<AssetView | null>(null);
  readonly priceSource = input.required<PriceSourceStatus>();
  readonly closed = output<void>();
  readonly saved = output<AssetDetail>();

  private readonly api = inject(InvestmentsApi);
  protected readonly assetTypes = ASSET_TYPES;
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).group({
    ticker: ['', [Validators.required, Validators.maxLength(20)]],
    isin: ['', Validators.pattern(/^$|^[A-Za-z]{2}[A-Za-z0-9]{9}[0-9]$/)],
    name: [''],
    assetType: ['STOCK' as AssetType],
    currentPrice: [null as number | null],
    autoPrice: [false],
    priceSymbol: ['']
  });

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.value });
  protected readonly locked = computed(() => !!this.values().autoPrice);

  constructor() {
    effect(() => {
      const a = this.asset();
      if (!a) return;
      this.error.set(null);
      this.form.reset({
        ticker: a.ticker, isin: a.isin ?? '', name: a.name, assetType: a.assetType,
        currentPrice: a.currentPrice, autoPrice: a.autoPrice, priceSymbol: a.priceSymbol ?? ''
      });
      this.syncLock(a.autoPrice);
    });
    this.form.controls.autoPrice.valueChanges.subscribe(on => this.syncLock(!!on));
  }

  private syncLock(on: boolean): void {
    const control = this.form.controls.currentPrice;
    if (on) control.disable({ emitEvent: false });
    else control.enable({ emitEvent: false });
  }

  protected async submit(): Promise<void> {
    const a = this.asset();
    if (!a) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: AssetUpdateRequest = {
      ticker: v.ticker!.trim(),
      isin: v.isin?.trim().toUpperCase() || null,
      name: v.name?.trim() || null,
      assetType: v.assetType!,
      currentPrice: v.autoPrice ? null : v.currentPrice ?? null,
      autoPrice: !!v.autoPrice,
      priceSymbol: v.priceSymbol?.trim() || null
    };
    this.saving.set(true);
    this.error.set(null);
    try {
      this.saved.emit(await firstValueFrom(this.api.updateAsset(a.id, body)));
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
