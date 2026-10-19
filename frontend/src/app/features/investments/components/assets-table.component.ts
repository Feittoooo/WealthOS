import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ASSET_TYPES, AssetType, AssetView } from '../../../core/api/models';
import { FORMAT_PIPES } from '../../../core/format/format.pipes';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { IconComponent } from '../../../shared/ui/icon.component';
import { SortButtonComponent } from '../../../shared/ui/sort-button.component';
import { TableSort } from '../../../shared/ui/table-sort';

/**
 * One row per asset with everything added up. Clicking a row opens the asset (its operations
 * and actions); the table itself only displays, sorts and filters.
 */
@Component({
  selector: 'app-assets-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgClass, FormsModule, TranslatePipe, ...FORMAT_PIPES, IconComponent, SortButtonComponent],
  templateUrl: './assets-table.component.html'
})
export class AssetsTableComponent {
  readonly assets = input.required<AssetView[]>();
  readonly opened = output<AssetView>();

  protected readonly assetTypes = ASSET_TYPES;
  protected readonly sort = new TableSort<AssetView>('marketValue');
  protected search = '';
  protected readonly searchTerm = signal('');
  protected readonly typeFilter = signal<AssetType | ''>('');
  protected readonly showClosed = signal(false);

  protected readonly closedCount = computed(() => this.assets().filter(a => !a.open).length);

  protected readonly rows = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const type = this.typeFilter();
    const closed = this.showClosed();
    return this.sort.apply(this.assets()
      .filter(a => closed || a.open)
      .filter(a => !type || a.assetType === type)
      .filter(a => !term || `${a.ticker} ${a.isin ?? ''} ${a.name} ${a.accountName}`.toLowerCase().includes(term)));
  });

  protected readonly filtered = computed(() => this.rows().length !== this.assets().filter(a => a.open).length);

  protected open(asset: AssetView): void {
    this.opened.emit(asset);
  }
}
