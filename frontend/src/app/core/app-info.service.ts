import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppInfo } from './api/models';
import { DebugLogService } from './debug/debug-log.service';
import { FormatService } from './format/format.service';
import { I18nService } from './i18n/i18n.service';

/** Loaded once before the first screen: server settings plus the texts for the chosen language. */
@Injectable({ providedIn: 'root' })
export class AppInfoService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  private readonly format = inject(FormatService);
  private readonly debug = inject(DebugLogService);

  readonly info = signal<AppInfo | null>(null);

  async init(): Promise<void> {
    const [info] = await Promise.all([
      firstValueFrom(this.http.get<AppInfo>('/api/app')),
      this.i18n.use(this.i18n.language())
    ]);
    this.info.set(info);
    this.format.numberLocale.set(info.numberLocale);
    this.format.baseCurrency.set(info.baseCurrency);
    this.debug.info('app', 'Started', info);
  }

  debugEnabled(): boolean {
    return this.info()?.debugEnabled ?? false;
  }
}
