// project import
import en from 'src/assets/i18n/en.json';
import ar from 'src/assets/i18n/ar.json';
import fr from 'src/assets/i18n/fr.json';
import ro from 'src/assets/i18n/ro.json';
import cn from 'src/assets/i18n/cn.json';

// third party
import { TranslateLoader } from '@ngx-translate/core';

// angular import
import { Observable, of } from 'rxjs';

// Support for nested translation objects
type TranslationValue = string | { [key: string]: TranslationValue };
type TranslationData = { [key: string]: TranslationValue };

export class CustomTranslateLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<TranslationData> {
    switch (lang) {
      case 'ar':
        return of(ar as TranslationData);
      case 'fr':
        return of(fr as TranslationData);
      case 'ro':
        return of(ro as TranslationData);
      case 'cn':
        return of(cn as TranslationData);
      default:
        return of(en as TranslationData);
    }
  }
}
