import { ChangeDetectionStrategy, Component, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { IconComponent } from './icon.component';

/**
 * The price lock. On: the current price comes from the price source and updates every day.
 * Off: the price is typed by hand and never changes on its own. Works with formControlName.
 */
@Component({
  selector: 'app-lock-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, IconComponent],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => LockToggleComponent), multi: true }],
  template: `
    <button type="button" class="lock-toggle" [class.is-on]="value()" [disabled]="disabled()"
            [attr.aria-pressed]="value()" [title]="(value() ? 'lock.onHint' : 'lock.offHint') | t"
            (click)="toggle()">
      <app-icon [name]="value() ? 'lock' : 'unlock'" />
      <span>{{ (value() ? 'lock.on' : 'lock.off') | t }}</span>
    </button>
  `
})
export class LockToggleComponent implements ControlValueAccessor {
  readonly value = signal(false);
  readonly disabled = signal(false);
  private onChange: (v: boolean) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  toggle(): void {
    this.value.update(v => !v);
    this.onChange(this.value());
    this.onTouched();
  }

  writeValue(v: boolean | null): void {
    this.value.set(!!v);
  }

  registerOnChange(fn: (v: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }
}
