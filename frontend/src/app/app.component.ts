import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogComponent } from './layout/confirm-dialog.component';
import { SidebarComponent } from './layout/sidebar.component';
import { ToastsComponent } from './layout/toasts.component';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, SidebarComponent, ToastsComponent, ConfirmDialogComponent],
  template: `
    <div class="shell">
      <app-sidebar />
      <main class="shell__main">
        <router-outlet />
      </main>
    </div>
    <app-toasts />
    <app-confirm-dialog />
  `
})
export class AppComponent {}
