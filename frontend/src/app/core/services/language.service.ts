import { Injectable, inject, signal, computed, effect } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';

export type SupportedLanguage = 'ar' | 'en';
export type Direction = 'rtl' | 'ltr';

const LANGUAGE_STORAGE_KEY = 'erp_language';
const DEFAULT_LANGUAGE: SupportedLanguage = 'ar';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  private translate = inject(TranslateService);
  private themeService = inject(ThemeService);

  // Signals for reactive state
  private currentLanguageSignal = signal<SupportedLanguage>(DEFAULT_LANGUAGE);
  
  /** Incremented on every language change — components can watch this to re-fetch data */
  readonly languageVersion = signal(0);

  // Computed signals
  readonly currentLanguage = this.currentLanguageSignal.asReadonly();
  readonly isRTL = computed(() => this.currentLanguageSignal() === 'ar');
  readonly direction = computed<Direction>(() => this.isRTL() ? 'rtl' : 'ltr');
  readonly isArabic = computed(() => this.currentLanguageSignal() === 'ar');
  readonly isEnglish = computed(() => this.currentLanguageSignal() === 'en');

  constructor() {
    // Initialize from localStorage or default
    this.initializeLanguage();
    
    // Effect to sync RTL mode with theme service
    effect(() => {
      this.themeService.isRTLMode.set(this.isRTL());
      this.applyDirectionToDOM();
    });
  }

  /**
   * Initialize language from localStorage or use default
   */
  private initializeLanguage(): void {
    const savedLanguage = localStorage.getItem(LANGUAGE_STORAGE_KEY) as SupportedLanguage;
    const initialLanguage = this.isValidLanguage(savedLanguage) ? savedLanguage : DEFAULT_LANGUAGE;
    
    // Configure translate service
    this.translate.addLangs(['ar', 'en']);
    this.translate.setDefaultLang('ar');
    
    // Set initial language without triggering storage save
    this.currentLanguageSignal.set(initialLanguage);
    this.translate.use(initialLanguage);
    
    // Persist the initial language
    this.persistLanguage(initialLanguage);
  }

  /**
   * Switch to specified language
   */
  setLanguage(lang: SupportedLanguage): void {
    if (!this.isValidLanguage(lang) || lang === this.currentLanguageSignal()) {
      return;
    }

    this.currentLanguageSignal.set(lang);
    this.translate.use(lang);
    this.persistLanguage(lang);
    this.languageVersion.update(v => v + 1);
  }

  /**
   * Toggle between Arabic and English
   */
  toggleLanguage(): void {
    const newLang: SupportedLanguage = this.currentLanguageSignal() === 'en' ? 'ar' : 'en';
    this.setLanguage(newLang);
  }

  /**
   * Get localized value from bilingual object
   * @param nameAr Arabic name
   * @param nameEn English name
   * @returns Localized name based on current language
   */
  getLocalizedName(nameAr: string | undefined, nameEn: string | undefined): string {
    if (this.isArabic()) {
      return nameAr || nameEn || '';
    }
    return nameEn || nameAr || '';
  }

  /**
   * Get localized value from object with nameAr/nameEn properties
   */
  localize<T extends { nameAr?: string; nameEn?: string }>(item: T): string {
    return this.getLocalizedName(item.nameAr, item.nameEn);
  }

  /**
   * Apply direction attribute to DOM
   */
  private applyDirectionToDOM(): void {
    const dir = this.direction();
    const isRtl = this.isRTL();
    
    // Set dir and lang attributes
    document.documentElement.setAttribute('dir', dir);
    document.documentElement.setAttribute('lang', this.currentLanguageSignal());
    document.body.setAttribute('dir', dir);
    
    // Add/remove RTL/LTR classes (mantis theme specific)
    if (isRtl) {
      document.body.classList.add('rtl', 'mantis-rtl');
      document.body.classList.remove('ltr', 'mantis-ltr');
    } else {
      document.body.classList.add('ltr', 'mantis-ltr');
      document.body.classList.remove('rtl', 'mantis-rtl');
    }
    

  }

  /**
   * Persist language preference to localStorage
   */
  private persistLanguage(lang: SupportedLanguage): void {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, lang);
  }

  /**
   * Validate if language code is supported
   */
  private isValidLanguage(lang: string | null): lang is SupportedLanguage {
    return lang === 'ar' || lang === 'en';
  }

  /**
   * Get current language code
   */
  getCurrentLanguageCode(): SupportedLanguage {
    return this.currentLanguageSignal();
  }

  /**
   * Get display name for current language
   */
  getCurrentLanguageDisplay(): string {
    return this.isArabic() ? 'العربية' : 'English';
  }

  /**
   * Get opposite language display (for toggle button)
   */
  getToggleLanguageDisplay(): string {
    return this.isArabic() ? 'English' : 'العربية';
  }
}
