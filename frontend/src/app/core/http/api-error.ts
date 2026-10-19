import { HttpContext, HttpContextToken, HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../api/models';

/** Set on a request when the caller shows the error itself (e.g. inside a dialog). */
export const HANDLES_OWN_ERRORS = new HttpContextToken<boolean>(() => false);

// Server errors always have the ApiError shape; network problems and proxies may not.
export function toApiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' && 'code' in err.error) {
    return err.error as ApiError;
  }
  const status = err instanceof HttpErrorResponse ? err.status : 0;
  return {
    status,
    code: status === 0 ? 'error.network' : 'error.unexpected',
    message: '',
    requestId: err instanceof HttpErrorResponse ? err.headers?.get('X-Request-Id') ?? null : null,
    fields: []
  };
}

/** Options for calls whose errors are shown inline (dialogs) instead of as a toast. */
export function quietly(): { context: HttpContext } {
  return { context: new HttpContext().set(HANDLES_OWN_ERRORS, true) };
}
