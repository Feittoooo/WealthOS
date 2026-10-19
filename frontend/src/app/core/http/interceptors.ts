import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';
import { DebugLogService } from '../debug/debug-log.service';
import { I18nService } from '../i18n/i18n.service';
import { ToastService } from '../ui-state/toast.service';
import { HANDLES_OWN_ERRORS, toApiError } from './api-error';

// Same language as the UI, so server-side errors come back translated
export const languageInterceptor: HttpInterceptorFn = (req, next) => {
  const lang = inject(I18nService).language();
  return next(req.clone({ setHeaders: { 'Accept-Language': lang === 'es' ? 'es-ES' : 'en' } }));
};

// Gives each call an id that the server logs as well, and records it on the debug page
export const traceInterceptor: HttpInterceptorFn = (req, next) => {
  const debug = inject(DebugLogService);
  const requestId = Math.random().toString(36).slice(2, 10);
  const started = performance.now();
  const traced = req.clone({ setHeaders: { 'X-Request-Id': requestId } });
  return next(traced).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        debug.http({
          level: 'debug', message: `${req.method} ${req.urlWithParams}`, method: req.method,
          url: req.urlWithParams, status: event.status, durationMs: Math.round(performance.now() - started),
          requestId, detail: req.body ?? undefined
        });
      }
    }),
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        debug.http({
          level: err.status >= 500 || err.status === 0 ? 'error' : 'warn',
          message: `${req.method} ${req.urlWithParams}`, method: req.method, url: req.urlWithParams,
          status: err.status, durationMs: Math.round(performance.now() - started), requestId,
          detail: { request: req.body ?? null, response: err.error }
        });
      }
      return throwError(() => err);
    })
  );
};

// Anything not handled by the caller ends up as a toast with the server's (translated) message
export const errorToastInterceptor: HttpInterceptorFn = (req, next) => {
  const toasts = inject(ToastService);
  const i18n = inject(I18nService);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && !req.context.get(HANDLES_OWN_ERRORS)) {
        const apiError = toApiError(err);
        const text = apiError.message || (i18n.has(apiError.code) ? i18n.t(apiError.code) : 'Error');
        toasts.error(text, apiError.requestId);
      }
      return throwError(() => err);
    })
  );
};
