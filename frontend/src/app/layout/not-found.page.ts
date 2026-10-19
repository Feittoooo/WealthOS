import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '../core/i18n/translate.pipe';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';

@Component({
  selector: 'app-not-found',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TranslatePipe, EmptyStateComponent],
  template: `
    <section class="panel">
      <app-empty-state titleKey="error.pageNotFound" textKey="error.pageNotFoundText">
        <a class="button" routerLink="/">{{ 'error.back' | t }}</a>
      </app-empty-state>
    </section>
  `
})
export class NotFoundPage {}
