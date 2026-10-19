import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AccountRequest, AssetDetail, AssetUpdateRequest, BuyRequest, BuyResult, OperationUpdateRequest, PortfolioOverview,
  SellRequest, SellResult, TransferRequest, TransferResult
} from '../../core/api/models';
import { quietly } from '../../core/http/api-error';

// Writes are "quiet": the dialogs show their own errors next to the fields.
@Injectable({ providedIn: 'root' })
export class InvestmentsApi {
  private readonly http = inject(HttpClient);

  overview(): Observable<PortfolioOverview> {
    return this.http.get<PortfolioOverview>('/api/portfolio');
  }

  createAccount(body: AccountRequest): Observable<{ id: number }> {
    return this.http.post<{ id: number }>('/api/accounts', body, quietly());
  }

  updateAccount(id: number, body: AccountRequest): Observable<{ id: number }> {
    return this.http.put<{ id: number }>(`/api/accounts/${id}`, body, quietly());
  }

  deleteAccount(id: number): Observable<void> {
    return this.http.delete<void>(`/api/accounts/${id}`);
  }

  // --- assets (the groups)

  asset(id: number): Observable<AssetDetail> {
    return this.http.get<AssetDetail>(`/api/assets/${id}`);
  }

  buy(body: BuyRequest): Observable<BuyResult> {
    return this.http.post<BuyResult>('/api/assets/buy', body, quietly());
  }

  updateAsset(id: number, body: AssetUpdateRequest): Observable<AssetDetail> {
    return this.http.put<AssetDetail>(`/api/assets/${id}`, body, quietly());
  }

  deleteAsset(id: number): Observable<void> {
    return this.http.delete<void>(`/api/assets/${id}`);
  }

  sell(id: number, body: SellRequest): Observable<SellResult> {
    return this.http.post<SellResult>(`/api/assets/${id}/sell`, body, quietly());
  }

  transfer(id: number, body: TransferRequest): Observable<TransferResult> {
    return this.http.post<TransferResult>(`/api/assets/${id}/transfer`, body, quietly());
  }

  // --- single operations inside an asset

  updateOperation(id: number, body: OperationUpdateRequest): Observable<AssetDetail> {
    return this.http.put<AssetDetail>(`/api/operations/${id}`, body, quietly());
  }

  // null when that was the last operation and the asset is gone
  deleteOperation(id: number): Observable<AssetDetail | null> {
    return this.http.delete<AssetDetail | null>(`/api/operations/${id}`);
  }
}
