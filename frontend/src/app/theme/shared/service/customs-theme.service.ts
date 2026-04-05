import { Injectable, signal, effect, inject, DOCUMENT } from '@angular/core';
import { MantisConfig } from 'src/app/app-config';

const STORAGE_KEYS = {
  THEME_COLOR: 'erp_theme_color',
  DARK_MODE: 'erp_dark_mode',
  CONTAINER_MODE: 'erp_container_mode'
} as const;

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private document = inject(DOCUMENT);

  // ── Signals ──────────────────────────────────────────────
  customsTheme = signal<string>(this.loadString(STORAGE_KEYS.THEME_COLOR, MantisConfig.theme_color));
  isDarkMode = signal<boolean>(this.loadBoolean(STORAGE_KEYS.DARK_MODE, MantisConfig.isDarkMode));
  isRTLMode = signal<boolean>(false);
  isContainerMode = signal<boolean>(this.loadBoolean(STORAGE_KEYS.CONTAINER_MODE, MantisConfig.isBox_container));

  constructor() {
    // Sync theme color preset to DOM + localStorage
    effect(() => {
      const preset = this.customsTheme();
      const body = this.document.body;
      // Remove all existing presets
      for (let i = 1; i <= 9; i++) {
        body.part.remove(`preset-${i}`);
      }
      if (preset) {
        body.part.add(preset);
        localStorage.setItem(STORAGE_KEYS.THEME_COLOR, preset);
      }
    });

    // Sync dark mode to DOM + localStorage
    effect(() => {
      const dark = this.isDarkMode();
      if (dark) {
        this.document.body.classList.add('mantis-dark');
      } else {
        this.document.body.classList.remove('mantis-dark');
      }
      localStorage.setItem(STORAGE_KEYS.DARK_MODE, String(dark));
    });

    // Sync container mode to DOM + localStorage
    effect(() => {
      const container = this.isContainerMode();
      const el = this.document.querySelector('.coded-content');
      if (container) {
        el?.classList.add('container');
      } else {
        el?.classList.remove('container');
      }
      localStorage.setItem(STORAGE_KEYS.CONTAINER_MODE, String(container));
    });
  }

  // ── Public Methods ───────────────────────────────────────

  setThemeColor(preset: string): void {
    this.customsTheme.set(preset);
  }

  toggleDarkMode(): void {
    this.isDarkMode.update(v => !v);
  }

  toggleContainerMode(): void {
    this.isContainerMode.update(v => !v);
  }

  // ── Private Helpers ──────────────────────────────────────

  private loadString(key: string, fallback: string): string {
    return localStorage.getItem(key) ?? fallback;
  }

  private loadBoolean(key: string, fallback: boolean): boolean {
    const stored = localStorage.getItem(key);
    if (stored === null) return fallback;
    return stored === 'true';
  }
}
