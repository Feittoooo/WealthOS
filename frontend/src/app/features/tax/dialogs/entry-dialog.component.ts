import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiError, TaxEntryRequest, TaxEntryType, TaxEntryView, TaxSettings } from '../../../core/api/models';
import { MoneyPipe } from '../../../core/format/format.pipes';
import { toApiError } from '../../../core/http/api-error';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { SegmentedComponent } from '../../../shared/ui/segmented.component';
import { TaxApi } from '../tax.api';

const EXPENSE_CATEGORIES = 10;
const INCOME_CATEGORIES = 4;

@Component({
  selector: 'app-entry-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, MoneyPipe, ModalComponent, SegmentedComponent, FormErrorComponent],
  templateUrl: './entry-dialog.component.html'
})
export class EntryDialogComponent {
  readonly open = input(false);
  readonly entry = input<TaxEntryView | null>(null);
  readonly settings = input.required<TaxSettings>();
  readonly defaultDate = input.required<string>();
  readonly closed = output<void>();
  readonly saved = output<TaxEntryView>();

  private readonly api = inject(TaxApi);
  private readonly i18n = inject(I18nService);
  protected readonly typeOptions = [
    { value: 'INGRESO' as TaxEntryType, labelKey: 'tax.typeIncome' },
    { value: 'GASTO' as TaxEntryType, labelKey: 'tax.typeExpense' }
  ];
  protected readonly ivaRates = [21, 10, 4, 0];
  protected readonly irpfRates = [15, 7, 0];
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    type: ['INGRESO' as TaxEntryType],
    date: ['', Validators.required],
    concept: ['', [Validators.required, Validators.maxLength(160)]],
    counterparty: [''],
    category: [''],
    baseAmount: [0, [Validators.required, Validators.min(0.01)]],
    ivaRate: [21],
    irpfRetentionRate: [15],
    deductible: [true]
  });

  private readonly values = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  protected readonly isIncome = computed(() => this.values().type === 'INGRESO');
  protected readonly categories = computed(() => {
    this.i18n.language();
    const income = this.isIncome();
    const count = income ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
    return Array.from({ length: count }, (_, i) => this.i18n.t(`category.${income ? 'income' : 'expense'}.${i + 1}`));
  });
  protected readonly preview = computed(() => {
    const v = this.values();
    const base = v.baseAmount ?? 0;
    const iva = base * (v.ivaRate ?? 0) / 100;
    const withheld = v.type === 'INGRESO' ? base * (v.irpfRetentionRate ?? 0) / 100 : 0;
    return { iva, withheld, total: base + iva - withheld };
  });

  constructor() {
    effect(() => {
      if (!this.open()) return;
      this.error.set(null);
      const e = this.entry();
      this.form.reset(e
        ? {
            type: e.type, date: e.date, concept: e.concept, counterparty: e.counterparty ?? '', category: e.category ?? '',
            baseAmount: e.baseAmount, ivaRate: e.ivaRate, irpfRetentionRate: e.irpfRetentionRate, deductible: e.deductible
          }
        : {
            type: 'INGRESO', date: this.defaultDate(), concept: '', counterparty: '', category: '', baseAmount: 0,
            ivaRate: 21, irpfRetentionRate: this.settings().defaultRetentionRate, deductible: true
          });
    });
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: TaxEntryRequest = {
      ...v,
      counterparty: v.counterparty || null,
      category: v.category || null,
      irpfRetentionRate: v.type === 'INGRESO' ? v.irpfRetentionRate : 0,
      deductible: v.type === 'GASTO' && v.deductible
    };
    const existing = this.entry();
    this.saving.set(true);
    this.error.set(null);
    try {
      this.saved.emit(await firstValueFrom(existing ? this.api.updateEntry(existing.id, body) : this.api.createEntry(body)));
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
