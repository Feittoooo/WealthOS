import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ActivityItem, DashboardView } from '../../core/api/models';
import { FORMAT_PIPES } from '../../core/format/format.pipes';
import { todayIso } from '../../core/format/format.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { DataEventsService } from '../../core/ui-state/data-events.service';
import { ToastService } from '../../core/ui-state/toast.service';
import { DeadlineListComponent } from '../../shared/ui/deadline-list.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { AllocationComponent } from '../../shared/ui/allocation.component';
import { DataApi } from '../data/data.api';
import { DashboardApi } from './dashboard.api';
import { NetWorthChartComponent } from './net-worth-chart.component';

@Component({
  selector: 'app-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, RouterLink, TranslatePipe, ...FORMAT_PIPES, NetWorthChartComponent, DeadlineListComponent,
    EmptyStateComponent, AllocationComponent],
  templateUrl: './dashboard.page.html'
})
export class DashboardPage {
  private readonly api = inject(DashboardApi);
  private readonly dataApi = inject(DataApi);
  private readonly i18n = inject(I18nService);
  private readonly toasts = inject(ToastService);
  protected readonly events = inject(DataEventsService);

  protected readonly view = signal<DashboardView | null>(null);
  protected readonly loading = signal(true);
  protected readonly today = todayIso();

  constructor() {
    effect(() => {
      this.events.version();
      untracked(() => void this.load());
    });
  }

  protected activityText(a: ActivityItem): string {
    return a.messageCode ? this.i18n.t(a.messageCode, a.args) : a.description;
  }

  protected async loadSample(): Promise<void> {
    await firstValueFrom(this.dataApi.loadSample());
    this.toasts.success(this.i18n.t('data.sampleLoaded'));
    this.events.changed();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.view.set(await firstValueFrom(this.api.get()));
    } finally {
      this.loading.set(false);
    }
  }
}
