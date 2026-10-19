import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TaxEntryRequest, TaxEntryView, TaxOverview, TaxSettings } from '../../core/api/models';
import { quietly } from '../../core/http/api-error';

@Injectable({ providedIn: 'root' })
export class TaxApi {
  private readonly http = inject(HttpClient);

  overview(year: number | null, quarter: number | null): Observable<TaxOverview> {
    let params = new HttpParams();
    if (year) params = params.set('year', year);
    if (quarter) params = params.set('quarter', quarter);
    return this.http.get<TaxOverview>('/api/tax', { params });
  }

  createEntry(body: TaxEntryRequest): Observable<TaxEntryView> {
    return this.http.post<TaxEntryView>('/api/tax/entries', body, quietly());
  }

  updateEntry(id: number, body: TaxEntryRequest): Observable<TaxEntryView> {
    return this.http.put<TaxEntryView>(`/api/tax/entries/${id}`, body, quietly());
  }

  deleteEntry(id: number): Observable<void> {
    return this.http.delete<void>(`/api/tax/entries/${id}`);
  }

  saveSettings(body: TaxSettings): Observable<TaxSettings> {
    return this.http.put<TaxSettings>('/api/tax/settings', body, quietly());
  }

  showOnDashboard(show: boolean): Observable<TaxSettings> {
    return this.http.put<TaxSettings>('/api/tax/settings/show-on-dashboard', { show });
  }

  toggleCuota(month: string): Observable<{ paid: boolean }> {
    return this.http.post<{ paid: boolean }>(`/api/tax/cuota/${month}/toggle`, null);
  }
}
