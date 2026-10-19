import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AppInfoService } from '../core/app-info.service';
import { I18nService, Language } from '../core/i18n/i18n.service';
import { TranslatePipe } from '../core/i18n/translate.pipe';
import { ThemeService } from '../core/ui-state/theme.service';
import { DataMenuComponent } from '../features/data/data-menu.component';
import { IconComponent } from '../shared/ui/icon.component';

@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, TranslatePipe, IconComponent, DataMenuComponent],
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent {
  protected readonly app = inject(AppInfoService);
  protected readonly i18n = inject(I18nService);
  protected readonly theme = inject(ThemeService);

  protected setLanguage(lang: Language): void {
    if (lang !== this.i18n.language()) void this.i18n.use(lang);
  }
}
