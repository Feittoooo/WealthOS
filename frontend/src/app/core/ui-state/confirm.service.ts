import { Injectable, signal } from '@angular/core';

export interface ConfirmRequest {
  // message keys + arguments, translated by the dialog
  titleKey: string;
  titleArgs?: unknown[];
  messageKey: string;
  messageArgs?: unknown[];
  confirmKey: string;
  danger?: boolean;
}

interface Pending extends ConfirmRequest {
  resolve: (ok: boolean) => void;
}

/** const ok = await confirm.ask({...}); the dialog itself lives once in the app shell. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly pending = signal<Pending | null>(null);

  ask(request: ConfirmRequest): Promise<boolean> {
    this.pending()?.resolve(false);
    return new Promise(resolve => this.pending.set({ ...request, resolve }));
  }

  answer(ok: boolean): void {
    const current = this.pending();
    this.pending.set(null);
    current?.resolve(ok);
  }
}
