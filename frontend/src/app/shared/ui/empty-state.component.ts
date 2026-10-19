import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    <div class="empty">
      <strong>{{ titleKey() | t }}</strong>
      @if (textKey()) { <p>{{ textKey() | t }}</p> }
      <div class="empty__actions"><ng-content /></div>
    </div>
  `
})
export class EmptyStateComponent {
  readonly titleKey = input.required<string>();
  readonly textKey = input<string>('');
}
