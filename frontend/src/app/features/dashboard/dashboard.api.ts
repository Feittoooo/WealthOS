import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DashboardView, NetWorthRange, NetWorthSeries } from '../../core/api/models';

@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  get(): Observable<DashboardView> {
    return this.http.get<DashboardView>('/api/dashboard');
  }

  netWorth(range: NetWorthRange, from?: string | null, to?: string | null): Observable<NetWorthSeries> {
    let params = new HttpParams().set('range', range);
    if (range === 'CUSTOM') {
      if (from) params = params.set('from', from);
      if (to) params = params.set('to', to);
    }
    return this.http.get<NetWorthSeries>('/api/networth', { params });
  }
}
