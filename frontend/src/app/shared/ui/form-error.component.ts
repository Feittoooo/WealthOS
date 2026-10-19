import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ApiError } from '../../core/api/models';

/** Error box inside a dialog: the server's message plus one line per invalid field. */
@Component({
  selector: 'app-form-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as e) {
      <div class="form-error" role="alert">
        <p>{{ e.message }}</p>
        @if (e.fields.length) {
          <ul>
            @for (f of e.fields; track f.field) { <li>{{ f.message }}</li> }
          </ul>
        }
        @if (e.requestId) { <small>ref {{ e.requestId }}</small> }
      </div>
    }
  `
})
export class FormErrorComponent {
  readonly error = input<ApiError | null>(null);
}
