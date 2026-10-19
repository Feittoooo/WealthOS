import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiError, TaxSettings } from '../../../core/api/models';
import { toApiError } from '../../../core/http/api-error';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { FormErrorComponent } from '../../../shared/ui/form-error.component';
import { ModalComponent } from '../../../shared/ui/modal.component';
import { TaxApi } from '../tax.api';

@Component({
  selector: 'app-settings-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslatePipe, ModalComponent, FormErrorComponent],
  template: `
    <app-modal [open]="open()" (closed)="closed.emit()" [title]="'tax.settingsTitle' | t" [subtitle]="'tax.settingsText' | t">
      <form class="dialog__form" [formGroup]="form" (ngSubmit)="submit()">
        <div class="dialog__body form">
          <app-form-error [error]="error()" />
          <label class="field">
            <span>{{ 'tax.monthlyCuota' | t }}</span>
            <input type="number" step="0.01" min="0" formControlName="monthlyCuota" class="num">
            <small>{{ 'tax.monthlyCuotaHint' | t }}</small>
          </label>
          <label class="field">
            <span>{{ 'tax.defaultRetention' | t }}</span>
            <select formControlName="defaultRetentionRate">
              @for (r of [15, 7, 0]; track r) { <option [ngValue]="r">{{ 'tax.ret' + r | t }}</option> }
            </select>
          </label>
          <label class="check">
            <input type="checkbox" formControlName="cuotaDeductible">
            <span>{{ 'tax.deductCuota' | t }}<small>{{ 'tax.deductCuotaHint' | t }}</small></span>
          </label>
          <label class="check">
            <input type="checkbox" formControlName="showOnDashboard">
            <span>{{ 'tax.showOnDashboard' | t }}<small>{{ 'tax.showOnDashboardHint' | t }}</small></span>
          </label>
        </div>
        <div class="dialog__footer">
          <button type="button" class="button" (click)="closed.emit()">{{ 'common.cancel' | t }}</button>
          <button type="submit" class="button button--primary" [disabled]="saving()">{{ 'common.save' | t }}</button>
        </div>
      </form>
    </app-modal>
  `
})
export class SettingsDialogComponent {
  readonly open = input(false);
  readonly settings = input.required<TaxSettings>();
  readonly closed = output<void>();
  readonly saved = output<void>();

  private readonly api = inject(TaxApi);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    monthlyCuota: [0, [Validators.required, Validators.min(0)]],
    defaultRetentionRate: [15],
    cuotaDeductible: [true],
    showOnDashboard: [true]
  });

  constructor() {
    effect(() => {
      if (!this.open()) return;
      this.error.set(null);
      this.form.reset({ ...this.settings() });
    });
  }

  protected async submit(): Promise<void> {
    this.saving.set(true);
    try {
      await firstValueFrom(this.api.saveSettings(this.form.getRawValue()));
      this.saved.emit();
    } catch (err) {
      this.error.set(toApiError(err));
    } finally {
      this.saving.set(false);
    }
  }
}
