import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DebugLogService } from '../debug/debug-log.service';
import { formatMessage } from './message-format';

export type Language = 'en' | 'es';

const STORAGE_KEY = 'wealthos-lang';

/**
 * Texts come from the backend (GET /api/i18n/{lang}), which reads messages.properties and
 * messages_es.properties. Nothing user-facing is hard-coded in components.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly http = inject(HttpClient);
  private readonly debug = inject(DebugLogService);

  private readonly messages = signal<Record<string, string>>({});
  readonly language = signal<Language>(initialLanguage());
  // es-ES for Intl date formatting, plain "en" otherwise
  readonly dateLocale = computed(() => (this.language() === 'es' ? 'es-ES' : 'en-GB'));

  async use(lang: Language): Promise<void> {
    const bundle = await firstValueFrom(this.http.get<Record<string, string>>(`/api/i18n/${lang}`));
    this.messages.set(bundle);
    this.language.set(lang);
    document.documentElement.lang = lang;
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      // private mode, the choice just won't be remembered
    }
    this.debug.info('i18n', `Loaded ${Object.keys(bundle).length} texts for "${lang}"`);
  }

  t(key: string, args: readonly unknown[] = []): string {
    const pattern = this.messages()[key];
    if (pattern === undefined) {
      this.debug.warn('i18n', `Missing text: ${key}`);
      return `??${key}??`;
    }
    return formatMessage(pattern, args, this.dateLocale());
  }

  has(key: string): boolean {
    return key in this.messages();
  }
}

function initialLanguage(): Language {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'en' || saved === 'es') return saved;
  } catch {
    // ignore
  }
  return navigator.language?.toLowerCase().startsWith('es') ? 'es' : 'en';
}
