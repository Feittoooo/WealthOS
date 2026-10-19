import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { NgClass } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { TaxEntryType, TaxEntryView, TaxOverview } from '../../core/api/models';
import { FORMAT_PIPES } from '../../core/format/format.pipes';
import { FormatService } from '../../core/format/format.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ConfirmService } from '../../core/ui-state/confirm.service';
import { DataEventsService } from '../../core/ui-state/data-events.service';
import { ToastService } from '../../core/ui-state/toast.service';
import { DeadlineListComponent } from '../../shared/ui/deadline-list.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { EntryDialogComponent } from './dialogs/entry-dialog.component';
import { SettingsDialogComponent } from './dialogs/settings-dialog.component';
import { TaxApi } from './tax.api';

@Component({
  selector: 'app-tax-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, TranslatePipe, ...FORMAT_PIPES, DeadlineListComponent, EmptyStateComponent, IconComponent,
    EntryDialogComponent, SettingsDialogComponent],
  templateUrl: './tax.page.html'
})
export class TaxPage {
  private readonly api = inject(TaxApi);
  private readonly i18n = inject(I18nService);
  private readonly toasts = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly events = inject(DataEventsService);
  protected readonly fmt = inject(FormatService);

  protected readonly year = signal<number | null>(null);
  protected readonly quarter = signal<number | null>(null);
  protected readonly overview = signal<TaxOverview | null>(null);
  protected readonly typeFilter = signal<TaxEntryType | ''>('');
  protected readonly entryDialog = signal<{ entry: TaxEntryView | null } | null>(null);
  protected readonly settingsOpen = signal(false);

  protected readonly quarters = [1, 2, 3, 4];
  protected readonly years = computed(() => {
    const current = this.overview()?.currentYear ?? new Date().getFullYear();
    return [current + 1, current, current - 1, current - 2, current - 3];
  });
  protected readonly entries = computed(() => {
    const type = this.typeFilter();
    return (this.overview()?.entries ?? []).filter(e => !type || e.type === type);
  });

  constructor() {
    effect(() => {
      this.events.version();
      const y = this.year();
      const q = this.quarter();
      untracked(() => void this.load(y, q));
    });
  }

  protected selectQuarter(year: number, quarter: number): void {
    this.year.set(year);
    this.quarter.set(quarter);
  }

  protected async toggleDashboard(show: boolean): Promise<void> {
    await firstValueFrom(this.api.showOnDashboard(show));
    this.reload();
  }

  protected async toggleCuota(month: string): Promise<void> {
    await firstValueFrom(this.api.toggleCuota(month));
    this.reload();
  }

  protected entrySaved(entry: TaxEntryView): void {
    const editing = this.entryDialog()?.entry;
    this.entryDialog.set(null);
    this.toasts.success(this.i18n.t(editing ? 'flash.entryUpdated' : 'flash.entryAdded', [entry.concept]));
    // jump to the quarter of the entry so it's visible
    const [y, m] = entry.date.split('-').map(Number);
    this.selectQuarter(y, Math.floor((m - 1) / 3) + 1);
    this.reload();
  }

  protected settingsSaved(): void {
    this.settingsOpen.set(false);
    this.toasts.success(this.i18n.t('flash.settingsSaved'));
    this.reload();
  }

  protected async deleteEntry(entry: TaxEntryView): Promise<void> {
    const ok = await this.confirm.ask({
      titleKey: 'tax.deleteEntryTitle', messageKey: 'tax.deleteEntryMsg', messageArgs: [entry.concept],
      confirmKey: 'tax.deleteEntryConfirm', danger: true
    });
    if (!ok) return;
    await firstValueFrom(this.api.deleteEntry(entry.id));
    this.toasts.success(this.i18n.t('flash.entryDeleted'));
    this.reload();
  }

  protected monthLabel(month: string): string {
    return this.fmt.month(month, false);
  }

  private reload(): void {
    void this.load(this.year(), this.quarter());
  }

  private async load(year: number | null, quarter: number | null): Promise<void> {
    const o = await firstValueFrom(this.api.overview(year, quarter));
    this.overview.set(o);
  }
}
