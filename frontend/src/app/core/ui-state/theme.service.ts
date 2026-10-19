import { Injectable, signal } from '@angular/core';

export type Theme = 'dark' | 'light';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  // index.html already applied the saved theme before Angular started
  readonly theme = signal<Theme>(
    document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark'
  );

  toggle(): void {
    this.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  set(theme: Theme): void {
    this.theme.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem('wealthos-theme', theme);
    } catch {
      // ignore
    }
  }
}
