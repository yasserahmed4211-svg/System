// Angular import
import { Component, inject, effect } from '@angular/core';
import { CommonModule, Location, LocationStrategy } from '@angular/common';
import { RouterModule } from '@angular/router';

// Project import
import { MantisConfig } from 'src/app/app-config';
import { SharedModule } from '../../shared/shared.module';
import { ConfigurationComponent } from './configuration/configuration.component';
import { NavBarComponent } from './nav-bar/nav-bar.component';
import { NavigationComponent } from './navigation/navigation.component';
import { BreadcrumbComponent } from '../../shared/components/breadcrumb/breadcrumb.component';
import { LanguageService } from 'src/app/core/services/language.service';

@Component({
  selector: 'app-admin',
  imports: [CommonModule, SharedModule, NavigationComponent, NavBarComponent, ConfigurationComponent, RouterModule, BreadcrumbComponent],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss'],
  host: {
    '[attr.dir]': 'languageService.direction()',
    '[class.rtl]': 'languageService.isRTL()',
    '[class.ltr]': '!languageService.isRTL()'
  }
})
export class AdminLayout {
  private location = inject(Location);
  private locationStrategy = inject(LocationStrategy);
  languageService = inject(LanguageService);

  // public props
  mantisConfig;
  navCollapsed: boolean;
  navCollapsedMob: boolean;
  windowWidth: number;
  styleSelectorToggle!: boolean;

  // Constructor
  constructor() {
    this.mantisConfig = MantisConfig;

    let current_url = this.location.path();
    const baseHref = this.locationStrategy.getBaseHref();
    if (baseHref) {
      current_url = baseHref + this.location.path();
    }

    if (current_url === baseHref + '/layout/theme-compact' || current_url === baseHref + '/layout/box') {
      this.mantisConfig.isCollapseMenu = true;
    }

    this.windowWidth = window.innerWidth;
    this.navCollapsed = this.windowWidth >= 1025 ? MantisConfig.isCollapseMenu : false;
    this.navCollapsedMob = false;
  }

  // public method
  navMobClick() {
    if (this.navCollapsedMob && !document.querySelector('app-navigation.pc-sidebar')?.classList.contains('mob-open')) {
      this.navCollapsedMob = !this.navCollapsedMob;
      setTimeout(() => {
        this.navCollapsedMob = !this.navCollapsedMob;
      }, 100);
    } else {
      this.navCollapsedMob = !this.navCollapsedMob;
    }
    if (document.querySelector('app-navigation.pc-sidebar')?.classList.contains('navbar-collapsed')) {
      document.querySelector('app-navigation.pc-sidebar')?.classList.remove('navbar-collapsed');
    }
  }

  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeMenu();
    }
  }

  closeMenu() {
    if (document.querySelector('app-navigation.pc-sidebar')?.classList.contains('mob-open')) {
      document.querySelector('app-navigation.pc-sidebar')?.classList.remove('mob-open');
    }
  }
}
