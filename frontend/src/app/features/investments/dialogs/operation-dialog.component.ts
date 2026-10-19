import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiError, AssetDetail, OperationView } from '../../../core/api/models';
import { MoneyPipe } from '../../../core/format/format.pipes';
import { todayIso } from '../../../core/format/format.service';
import { toApiError } from '../../../core/http/api-error';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { InvestmentsApi } from '../investments.api';

/**
 * Fix one buy or sale: date, units, price. The server replays the whole asset afterwards, and
 * refuses the change if at some point you'd have sold more than you held.
 */
@Component({
  selector: 'app-operation-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, MoneyPipe, ModalComponent, FormErrorComponent],
  templateUrl: './operation-dialog.component.html'
})
export class OperationDialogComponent {
  readonly operation = input<OperationView | null>(null);
  readonly ticker = input('');
  readonly currency = input('EUR');
  readonly closed = output<void>();
  readonly saved = output<AssetDetail>();

  private readonly api = inject(InvestmentsApi);
  protected readonly today = todayIso();
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).group({
    date: ['', Validators.required],
    units: [null as number | null, [Validators.required, Validators.min(0.00000001)]],
    price: [null as number | null, [Validators.required, Validators.min(0.00000001)]]
  });

  constructor() {
    effect(() => {
      const op = this.operation();
      if (!op) return;
      this.error.set(null);
      this.form.reset({ date: op.date, units: op.units, price: op.price });
    });
  }

  protected async submit(): Promise<void> {
    const op = this.operation();
    if (!op) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    try {
      this.saved.emit(await firstValueFrom(this.api.updateOperation(op.id, { date: v.date!, units: v.units!, price: v.price! })));
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
