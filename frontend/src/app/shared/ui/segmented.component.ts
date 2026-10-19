import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

export interface SegmentOption<T> {
  value: T;
  labelKey: string;
}

/** A row of mutually exclusive buttons (units / money / all...). Works with formControlName. */
@Component({
  selector: 'app-segmented',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SegmentedComponent), multi: true }],
  template: `
    <div class="segmented" role="radiogroup">
      @for (option of options(); track option.value) {
        <button type="button" role="radio" [attr.aria-checked]="option.value === value()"
                [class.is-active]="option.value === value()" [disabled]="disabled()" (click)="select(option.value)">
          {{ option.labelKey | t }}
        </button>
      }
    </div>
  `
})
export class SegmentedComponent<T = string> implements ControlValueAccessor {
  readonly options = input.required<SegmentOption<T>[]>();
  readonly value = signal<T | null>(null);
  readonly disabled = signal(false);
  private onChange: (v: T) => void = () => undefined;

  select(v: T): void {
    this.value.set(v);
    this.onChange(v);
  }

  writeValue(v: T): void {
    this.value.set(v);
  }

  registerOnChange(fn: (v: T) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(): void {
    // nothing to track
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }
}
