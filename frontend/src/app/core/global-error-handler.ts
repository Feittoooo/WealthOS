import { ErrorHandler, Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DebugLogService } from './debug/debug-log.service';
import { I18nService } from './i18n/i18n.service';
import { ToastService } from './ui-state/toast.service';

/** Uncaught errors go to the debug log and show a short message instead of failing silently. */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly debug = inject(DebugLogService);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(I18nService);

  handleError(error: unknown): void {
    // HTTP errors were already logged and shown by the interceptors
    if (error instanceof HttpErrorResponse) return;
    const message = error instanceof Error ? error.message : String(error);
    this.debug.error('app', message, error instanceof Error ? error.stack : error);
    this.toasts.error(this.i18n.has('error.unexpected') ? this.i18n.t('error.unexpected') : message);
  }
}
