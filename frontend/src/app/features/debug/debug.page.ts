import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { JsonPipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { LoggerLevel, RequestTrace, ServerLogEntry } from '../../core/api/models';
import { AppInfoService } from '../../core/app-info.service';
import { DebugLogService } from '../../core/debug/debug-log.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ToastService } from '../../core/ui-state/toast.service';
import { DebugApi } from './debug.api';

type Tab = 'browser' | 'requests' | 'logs' | 'loggers' | 'system';

const LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'];
const POLL_MS = 3000;

/**
 * Everything needed to see what's going on without leaving the app: API calls made by the
 * browser, the same calls as the server saw them, the server log (live), log levels and
 * system info. A request id links a browser entry to its server log lines.
 */
@Component({
  selector: 'app-debug-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, JsonPipe, FormsModule, TranslatePipe],
  templateUrl: './debug.page.html'
})
export class DebugPage {
  private readonly api = inject(DebugApi);
  private readonly toasts = inject(ToastService);
  protected readonly i18n = inject(I18nService);
  protected readonly app = inject(AppInfoService);
  protected readonly browserLog = inject(DebugLogService);

  protected readonly tabs: Tab[] = ['browser', 'requests', 'logs', 'loggers', 'system'];
  protected readonly levels = LEVELS;
  protected readonly tab = signal<Tab>('browser');
  protected readonly requestFilter = signal('');
  protected readonly logLevel = signal('INFO');
  protected readonly expanded = signal<number | null>(null);

  protected readonly requests = signal<RequestTrace[]>([]);
  protected readonly logs = signal<ServerLogEntry[]>([]);
  protected readonly loggers = signal<LoggerLevel[]>([]);
  protected readonly info = signal<Record<string, unknown> | null>(null);
  protected readonly following = signal(true);

  protected readonly browserEntries = computed(() => {
    const id = this.requestFilter().trim();
    const list = this.browserLog.entries().slice().reverse();
    return id ? list.filter(e => e.requestId?.includes(id)) : list;
  });
  protected readonly serverLogs = computed(() => {
    const id = this.requestFilter().trim();
    const list = this.logs().slice().reverse();
    return id ? list.filter(e => e.requestId?.includes(id)) : list;
  });

  private lastLogId = 0;

  constructor() {
    const timer = setInterval(() => {
      if (this.tab() === 'logs' && this.following()) void this.pollLogs();
    }, POLL_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected async show(tab: Tab): Promise<void> {
    this.tab.set(tab);
    if (tab === 'requests') this.requests.set(await firstValueFrom(this.api.requests()));
    if (tab === 'logs') await this.pollLogs(true);
    if (tab === 'loggers') this.loggers.set(await firstValueFrom(this.api.loggers()));
    if (tab === 'system') this.info.set(await firstValueFrom(this.api.info()));
  }

  // click a request id anywhere to see everything that happened in that call
  protected follow(requestId: string | null | undefined): void {
    if (requestId) this.requestFilter.set(requestId);
  }

  protected async changeLogLevel(): Promise<void> {
    await this.pollLogs(true);
  }

  protected async setLoggerLevel(name: string, level: string): Promise<void> {
    await firstValueFrom(this.api.setLevel(name, level));
    this.loggers.set(await firstValueFrom(this.api.loggers()));
    this.toasts.success(this.i18n.t('debug.levelChanged', [name, level]));
  }

  protected async clearServer(): Promise<void> {
    await firstValueFrom(this.api.clearLogs());
    this.logs.set([]);
    this.requests.set([]);
    this.lastLogId = 0;
  }

  protected async refreshPrices(): Promise<void> {
    const r = await firstValueFrom(this.api.refreshPrices());
    this.toasts.success(this.i18n.t('debug.pricesRefreshed', [r.provider, r.checked, r.updated, r.missing]));
  }

  protected async reloadTexts(): Promise<void> {
    await this.i18n.use(this.i18n.language());
    this.toasts.success(this.i18n.t('debug.textsReloaded'));
  }

  // server times are UTC instants; show them in the browser's time zone
  protected clock(iso: string, millis = false): string {
    const d = new Date(iso);
    const time = d.toLocaleTimeString(this.i18n.dateLocale(), { hour12: false });
    return millis ? `${time}.${String(d.getMilliseconds()).padStart(3, '0')}` : time;
  }

  protected entries(record: Record<string, unknown>): [string, unknown][] {
    return Object.entries(record);
  }

  private async pollLogs(reset = false): Promise<void> {
    if (reset) {
      this.lastLogId = 0;
      this.logs.set([]);
    }
    const fresh = await firstValueFrom(this.api.logs(this.lastLogId, this.logLevel()));
    if (!fresh.length) return;
    this.lastLogId = fresh[fresh.length - 1].id;
    this.logs.update(list => [...list, ...fresh].slice(-1000));
  }
}
