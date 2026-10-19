import { Injectable, signal } from '@angular/core';

export type DebugLevel = 'debug' | 'info' | 'warn' | 'error';

export interface DebugEntry {
  id: number;
  time: Date;
  level: DebugLevel;
  source: string;
  message: string;
  // filled for API calls
  method?: string;
  url?: string;
  status?: number;
  durationMs?: number;
  requestId?: string;
  detail?: unknown;
}

const CAPACITY = 500;
const CONSOLE_KEY = 'wealthos-debug-console';

/**
 * Browser-side log shown on the Debug page: every API call (with the request id the server
 * logs too), translation misses, uncaught errors. Also mirrored to the console if enabled there.
 */
@Injectable({ providedIn: 'root' })
export class DebugLogService {
  private sequence = 0;
  readonly entries = signal<DebugEntry[]>([]);
  readonly mirrorToConsole = signal(readConsoleFlag());

  debug(source: string, message: string, detail?: unknown): void {
    this.add({ level: 'debug', source, message, detail });
  }

  info(source: string, message: string, detail?: unknown): void {
    this.add({ level: 'info', source, message, detail });
  }

  warn(source: string, message: string, detail?: unknown): void {
    this.add({ level: 'warn', source, message, detail });
  }

  error(source: string, message: string, detail?: unknown): void {
    this.add({ level: 'error', source, message, detail });
  }

  http(entry: Omit<DebugEntry, 'id' | 'time' | 'source'>): void {
    this.add({ ...entry, source: 'http' });
  }

  clear(): void {
    this.entries.set([]);
  }

  setConsoleMirror(on: boolean): void {
    this.mirrorToConsole.set(on);
    try {
      localStorage.setItem(CONSOLE_KEY, String(on));
    } catch {
      // ignore
    }
  }

  private add(partial: Omit<DebugEntry, 'id' | 'time'>): void {
    const entry: DebugEntry = { ...partial, id: ++this.sequence, time: new Date() };
    this.entries.update(list => {
      const next = list.length >= CAPACITY ? list.slice(1) : list.slice();
      next.push(entry);
      return next;
    });
    if (this.mirrorToConsole() || entry.level === 'error') {
      const fn = entry.level === 'error' ? console.error : entry.level === 'warn' ? console.warn : console.debug;
      fn(`[${entry.source}] ${entry.message}`, entry.detail ?? '');
    }
  }
}

function readConsoleFlag(): boolean {
  try {
    return localStorage.getItem(CONSOLE_KEY) === 'true';
  } catch {
    return false;
  }
}
