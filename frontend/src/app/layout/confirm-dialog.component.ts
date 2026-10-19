import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe } from '../core/i18n/translate.pipe';
import { ConfirmService } from '../core/ui-state/confirm.service';
import { ModalComponent } from '../shared/ui/modal.component';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModalComponent, TranslatePipe],
  template: `
    @let request = confirm.pending();
    <app-modal [open]="!!request" [title]="request ? i18n.t(request.titleKey, request.titleArgs ?? []) : ''"
               (closed)="confirm.answer(false)">
      @if (request) {
        <div class="dialog__body"><p class="m-0">{{ request.messageKey | t: (request.messageArgs ?? []) }}</p></div>
        <div class="dialog__footer">
          <button type="button" class="button" (click)="confirm.answer(false)">{{ 'common.cancel' | t }}</button>
          <button type="button" class="button" [class.button--danger]="request.danger" [class.button--primary]="!request.danger"
                  (click)="confirm.answer(true)">{{ request.confirmKey | t }}</button>
        </div>
      }
    </app-modal>
  `
})
export class ConfirmDialogComponent {
  protected readonly confirm = inject(ConfirmService);
  protected readonly i18n = inject(I18nService);
}
