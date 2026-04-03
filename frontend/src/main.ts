// angular import
import '@angular/localize/init';
import { enableProdMode, importProvidersFrom } from '@angular/core';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';

// project import
import { environment } from './environments/environment';
import { AppComponent } from './app/app.component';
import { AppRoutingModule } from './app/app-routing.module';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { AuthInterceptor, ErrorInterceptor } from 'src/app/core/interceptors';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { CustomTranslateLoader } from 'src/app/theme/shared/custom-translate-loader';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [
    importProvidersFrom(
      BrowserModule, 
      AppRoutingModule, 
      SharedModule,
      TranslateModule.forRoot({
        defaultLanguage: 'ar',
        loader: {
          provide: TranslateLoader,
          useClass: CustomTranslateLoader
        }
      })
    ),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    [provideHttpClient(withInterceptorsFromDi())]
  ]
}).catch((err) => console.error(err));
