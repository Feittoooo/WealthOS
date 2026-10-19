import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type IconName =
  | 'dashboard' | 'investments' | 'tax' | 'debug' | 'plus' | 'pencil' | 'trash' | 'lock' | 'unlock'
  | 'sell' | 'transfer' | 'sun' | 'moon' | 'database' | 'x' | 'download' | 'upload' | 'refresh'
  | 'alert' | 'check' | 'chevron';

// Paths from Lucide (MIT). Only the ones the app uses.
@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'icon', 'aria-hidden': 'true' },
  template: `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      @switch (name()) {
        @case ('dashboard') { <rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/> }
        @case ('investments') { <polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/> }
        @case ('tax') { <path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z"/><path d="M8 7h8M8 11h8M8 15h5"/> }
        @case ('debug') { <path d="M8 2l1.88 1.88M14.12 3.88 16 2M9 7.13v-1a3 3 0 1 1 6 0v1"/><path d="M12 20c-3.3 0-6-2.7-6-6v-3a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v3c0 3.3-2.7 6-6 6"/><path d="M12 20v-9M6.53 9C4.6 8.8 3 7.1 3 5M6 13H2M3 21c0-2.1 1.7-3.9 3.8-4M20.97 5c0 2.1-1.6 3.8-3.5 4M22 13h-4M17.2 17c2.1.1 3.8 1.9 3.8 4"/> }
        @case ('plus') { <path d="M12 5v14M5 12h14"/> }
        @case ('pencil') { <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/> }
        @case ('trash') { <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/> }
        @case ('lock') { <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/> }
        @case ('unlock') { <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/> }
        @case ('sell') { <path d="M12 2v14M5 9l7 7 7-7M5 22h14"/> }
        @case ('transfer') { <path d="M8 3 4 7l4 4M4 7h16M16 21l4-4-4-4M20 17H4"/> }
        @case ('sun') { <circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/> }
        @case ('moon') { <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/> }
        @case ('database') { <ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14a9 3 0 0 0 18 0V5"/><path d="M3 12a9 3 0 0 0 18 0"/> }
        @case ('x') { <path d="M18 6 6 18M6 6l12 12"/> }
        @case ('download') { <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/> }
        @case ('upload') { <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/> }
        @case ('refresh') { <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/> }
        @case ('alert') { <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"/><path d="M12 9v4M12 17h.01"/> }
        @case ('check') { <path d="M20 6 9 17l-5-5"/> }
        @case ('chevron') { <path d="m6 9 6 6 6-6"/> }
      }
    </svg>
  `
})
export class IconComponent {
  readonly name = input.required<IconName>();
}
