import { ChangeDetectionStrategy, Component, ElementRef, effect, input, output, viewChild } from '@angular/core';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { IconComponent } from './icon.component';

/**
 * Wrapper around the native <dialog> element (focus trap, Esc and backdrop for free).
 * The content decides its own body and footer, usually a <form class="dialog__form">.
 */
@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, IconComponent],
  template: `
    <dialog #dialog class="dialog" [class.dialog--wide]="wide()" (cancel)="dismiss($event)" (click)="backdrop($event)">
      <header class="dialog__header">
        <div>
          <h2>{{ title() }}</h2>
          @if (subtitle()) { <p>{{ subtitle() }}</p> }
        </div>
        <button type="button" class="icon-button" (click)="closed.emit()" [attr.aria-label]="'common.close' | t">
          <app-icon name="x" />
        </button>
      </header>
      @if (open()) { <ng-content /> }
    </dialog>
  `
})
export class ModalComponent {
  readonly open = input(false);
  readonly title = input('');
  readonly subtitle = input('');
  readonly wide = input(false);
  readonly closed = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const el = this.dialog().nativeElement;
      if (this.open() && !el.open) el.showModal();
      if (!this.open() && el.open) el.close();
    });
  }

  dismiss(event: Event): void {
    event.preventDefault();
    this.closed.emit();
  }

  // a click on the dialog element itself (not its content) is a click on the backdrop
  backdrop(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) this.closed.emit();
  }
}
