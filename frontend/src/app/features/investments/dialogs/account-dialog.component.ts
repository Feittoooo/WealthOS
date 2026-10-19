import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ACCOUNT_TYPES, AccountRequest, AccountType, AccountView, ApiError, CURRENCIES } from '../../../core/api/models';
import { toApiError } from '../../../core/http/api-error';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { SegmentedComponent } from '../../../shared/ui/segmented.component';
import { InvestmentsApi } from '../investments.api';

/** Create or edit an account. The balance is only the cash that isn't invested. */
@Component({
  selector: 'app-account-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, ModalComponent, SegmentedComponent, FormErrorComponent],
  templateUrl: './account-dialog.component.html'
})
export class AccountDialogComponent {
  readonly open = input(false);
  readonly account = input<AccountView | null>(null);
  readonly closed = output<void>();
  readonly saved = output<string>();

  private readonly api = inject(InvestmentsApi);
  protected readonly i18n = inject(I18nService);
  protected readonly currencies = CURRENCIES;
  protected readonly typeOptions = ACCOUNT_TYPES.map(t => ({ value: t, labelKey: `accountType.short.${t}` }));
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    type: ['BANK' as AccountType],
    institution: [''],
    currency: ['EUR'],
    balance: [0, Validators.required]
  });

  constructor() {
    effect(() => {
      if (!this.open()) return;
      const a = this.account();
      this.error.set(null);
      this.form.reset(a
        ? { name: a.name, type: a.type, institution: a.institution ?? '', currency: a.currency, balance: a.cashBalance }
        : { name: '', type: 'BANK', institution: '', currency: 'EUR', balance: 0 });
    });
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: AccountRequest = { ...v, institution: v.institution || null };
    const existing = this.account();
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(existing ? this.api.updateAccount(existing.id, body) : this.api.createAccount(body));
      this.saved.emit(v.name);
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
