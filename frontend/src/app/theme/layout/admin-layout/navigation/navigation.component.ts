// Angular import
import { Component, OnInit, effect, output, inject, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { NavContentComponent } from './nav-content/nav-content.component';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';
import { MantisConfig } from 'src/app/app-config';

@Component({
  selector: 'app-navigation',
  imports: [SharedModule, NavContentComponent, CommonModule],
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.scss']
})
export class NavigationComponent implements OnInit {
  private themeService = inject(ThemeService);

  // public props
  darkMode!: boolean;

  // media 1025 After Use Menu Open
  readonly NavCollapsedMob = output();
  readonly SubmenuCollapse = output();

  navCollapsedMob;
  windowWidth: number;
  themeLayout!: string;

  // Constructor
  constructor() {
    this.windowWidth = window.innerWidth;
    this.navCollapsedMob = false;
    effect(() => {
      const isDark = this.themeService.isDarkMode();
      untracked(() => this.isDarkTheme(isDark));
    });
  }

  ngOnInit() {
    this.darkMode = MantisConfig.isDarkMode;
  }

  private isDarkTheme(dark: boolean) {
    this.darkMode = dark;
  }
  // public method
  navCollapseMob() {
    if (this.windowWidth < 1025) {
      this.NavCollapsedMob.emit();
    }
  }

  navSubmenuCollapse() {
    document.querySelector('app-navigation.pc-sidebar')?.classList.add('coded-trigger');
  }
}
