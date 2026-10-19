import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

/**
 * {{ 'inv.newAccount' | t }}  or with arguments  {{ 'inv.avg' | t: [price] }}
 * Impure so it follows language changes; the lookup is a map access, so that's cheap.
 */
@Pipe({ name: 't', pure: false })
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(key: string | null | undefined, args: readonly unknown[] = []): string {
    return key ? this.i18n.t(key, args) : '';
  }
}
