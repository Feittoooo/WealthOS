import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AppInfoService } from './core/app-info.service';

// Pages are lazy loaded, so each feature is its own chunk.
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/dashboard/dashboard.page').then(m => m.DashboardPage)
  },
  {
    path: 'investments',
    loadComponent: () => import('./features/investments/investments.page').then(m => m.InvestmentsPage)
  },
  {
    path: 'tax',
    loadComponent: () => import('./features/tax/tax.page').then(m => m.TaxPage)
  },
  {
    path: 'debug',
    canMatch: [() => inject(AppInfoService).debugEnabled()],
    loadComponent: () => import('./features/debug/debug.page').then(m => m.DebugPage)
  },
  {
    path: '**',
    loadComponent: () => import('./layout/not-found.page').then(m => m.NotFoundPage)
  }
];
