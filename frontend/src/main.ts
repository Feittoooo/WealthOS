import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

bootstrapApplication(AppComponent, appConfig).catch(err => {
  console.error(err);
  // the app couldn't start (usually the backend isn't running); say so instead of a blank page
  document.body.innerHTML =
    '<p style="font:14px sans-serif;padding:24px">WealthOS could not start. Is the server running? ' +
    'See the browser console for details.</p>';
});
