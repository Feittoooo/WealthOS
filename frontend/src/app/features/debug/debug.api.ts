import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LoggerLevel, RequestTrace, ServerLogEntry } from '../../core/api/models';

@Injectable({ providedIn: 'root' })
export class DebugApi {
  private readonly http = inject(HttpClient);

  info(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>('/api/debug/info');
  }

  logs(after: number, level: string): Observable<ServerLogEntry[]> {
    return this.http.get<ServerLogEntry[]>('/api/debug/logs', {
      params: new HttpParams().set('after', after).set('level', level)
    });
  }

  clearLogs(): Observable<void> {
    return this.http.delete<void>('/api/debug/logs');
  }

  requests(): Observable<RequestTrace[]> {
    return this.http.get<RequestTrace[]>('/api/debug/requests');
  }

  loggers(): Observable<LoggerLevel[]> {
    return this.http.get<LoggerLevel[]>('/api/debug/loggers');
  }

  setLevel(name: string, level: string): Observable<LoggerLevel> {
    return this.http.put<LoggerLevel>(`/api/debug/loggers/${name}`, { level });
  }

  refreshPrices(): Observable<{ provider: string; checked: number; updated: number; missing: number }> {
    return this.http.post<{ provider: string; checked: number; updated: number; missing: number }>(
      '/api/debug/prices/refresh', null);
  }
}
