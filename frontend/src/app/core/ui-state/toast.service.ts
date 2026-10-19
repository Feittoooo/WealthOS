import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  kind: 'success' | 'error' | 'warning';
  // already translated
  text: string;
  requestId?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private sequence = 0;
  readonly toasts = signal<Toast[]>([]);

  success(text: string): void {
    this.push({ kind: 'success', text });
  }

  warning(text: string): void {
    this.push({ kind: 'warning', text });
  }

  error(text: string, requestId?: string | null): void {
    this.push({ kind: 'error', text, requestId });
  }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  private push(toast: Omit<Toast, 'id'>): void {
    const id = ++this.sequence;
    this.toasts.update(list => [...list, { ...toast, id }]);
    setTimeout(() => this.dismiss(id), toast.kind === 'error' ? 9000 : 5000);
  }
}
