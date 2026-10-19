import { ApplicationConfig, ErrorHandler, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { AppInfoService } from './core/app-info.service';
import { GlobalErrorHandler } from './core/global-error-handler';
import { errorToastInterceptor, languageInterceptor, traceInterceptor } from './core/http/interceptors';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    // order matters: language first, then the trace sees the final request, then error toasts
    provideHttpClient(withInterceptors([languageInterceptor, traceInterceptor, errorToastInterceptor])),
    provideAppInitializer(() => inject(AppInfoService).init()),
    { provide: ErrorHandler, useClass: GlobalErrorHandler }
  ]
};
