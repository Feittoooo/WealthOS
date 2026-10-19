import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AmountMode, ApiError, AssetView, SellResult } from '../../../core/api/models';
import { MoneyPipe, PricePipe, QtyPipe, TonePipe } from '../../../core/format/format.pipes';
import { todayIso } from '../../../core/format/format.service';
import { toApiError } from '../../../core/http/api-error';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { SegmentedComponent } from '../../../shared/ui/segmented.component';
import { InvestmentsApi } from '../investments.api';
import { sellPreview } from '../trade-preview';

/**
 * Full or partial sale. Price and date are optional: no date means today, no price means the
 * price on that date (today: the price we already have).
 */
@Component({
  selector: 'app-sell-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, MoneyPipe, PricePipe, QtyPipe, TonePipe, ModalComponent,
    SegmentedComponent, FormErrorComponent],
  templateUrl: './sell-dialog.component.html'
})
export class SellDialogComponent {
  readonly asset = input<AssetView | null>(null);
  readonly closed = output<void>();
  readonly sold = output<SellResult>();

  private readonly api = inject(InvestmentsApi);
  protected readonly today = todayIso();
  protected readonly modeOptions = [
    { value: 'ALL' as AmountMode, labelKey: 'amount.all' },
    { value: 'UNITS' as AmountMode, labelKey: 'amount.units' },
    { value: 'MONEY' as AmountMode, labelKey: 'amount.money' }
  ];
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).group({
    mode: ['ALL' as AmountMode],
    amount: [null as number | null],
    price: [null as number | null],
    date: [null as string | null],
    creditCash: [false]
  });

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  // the price the server will use if the field is left empty, when we can know it here
  protected readonly effectivePrice = computed(() => {
    const v = this.values();
    if (v.price) return v.price;
    const isToday = !v.date || v.date === this.today;
    return isToday ? this.asset()?.currentPrice ?? null : null;
  });

  protected readonly preview = computed(() => {
    const p = this.asset();
    const v = this.values();
    if (!p) return null;
    return sellPreview(v.mode ?? 'ALL', v.amount ?? null, p.quantity, p.avgBuyPrice, this.effectivePrice());
  });

  constructor() {
    effect(() => {
      if (!this.asset()) return;
      this.error.set(null);
      this.form.reset({ mode: 'ALL', amount: null, price: null, date: null, creditCash: false });
    });
  }

  protected async submit(): Promise<void> {
    const p = this.asset();
    if (!p) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    try {
      this.sold.emit(await firstValueFrom(this.api.sell(p.id, {
        mode: v.mode!,
        amount: v.mode === 'ALL' ? null : v.amount ?? null,
        price: v.price ?? null,
        date: v.date || null,
        creditCash: !!v.creditCash
      })));
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
