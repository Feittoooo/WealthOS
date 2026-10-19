import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, viewChild } from '@angular/core';
import { TranslatePipe } from '../core/i18n/translate.pipe';
import { ToastService } from '../core/ui-state/toast.service';
import { IconComponent } from '../shared/ui/icon.component';

@Component({
  selector: 'app-toasts',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent, TranslatePipe],
  template: `
    <div #stack class="toasts" popover="manual" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div class="toast" [class]="'toast toast--' + toast.kind" role="status">
          <span>{{ toast.text }}</span>
          @if (toast.requestId) { <small class="muted">ref {{ toast.requestId }}</small> }
          <button type="button" class="icon-button" (click)="toasts.dismiss(toast.id)" [attr.aria-label]="'common.close' | t">
            <app-icon name="x" />
          </button>
        </div>
      }
    </div>
  `
})
export class ToastsComponent {
  protected readonly toasts = inject(ToastService);
  private readonly stack = viewChild.required<ElementRef<HTMLElement>>('stack');

  constructor() {
    // A popover lives in the top layer, so toasts show above open dialogs too. Showing it again
    // on every new toast puts it back on top of any dialog opened in the meantime.
    effect(() => {
      const el = this.stack().nativeElement;
      const any = this.toasts.toasts().length > 0;
      if (el.matches(':popover-open')) el.hidePopover();
      if (any) el.showPopover();
    });
  }
}
