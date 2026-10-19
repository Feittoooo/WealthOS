import { Injectable, signal } from '@angular/core';

/**
 * Bumped after anything that replaces data wholesale (import, sample data, delete all),
 * so whichever page is open reloads.
 */
@Injectable({ providedIn: 'root' })
export class DataEventsService {
  readonly version = signal(0);

  changed(): void {
    this.version.update(v => v + 1);
  }
}
