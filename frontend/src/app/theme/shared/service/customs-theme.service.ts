import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  customsTheme = signal<string>('');
  isDarkMode = signal<boolean>(false);
  isRTLMode = signal<boolean>(false);
  isContainerMode = signal<boolean>(false);
}
