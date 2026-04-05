// Angular import
import { Component, inject, effect, HostListener, OnDestroy } from '@angular/core';
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
import { LayoutStateService } from '../../shared/service/layout-state.service';

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
export class AdminLayout implements OnDestroy {
  private location = inject(Location);
  private locationStrategy = inject(LocationStrategy);
  languageService = inject(LanguageService);
  readonly layout = inject(LayoutStateService);

  // public props
  mantisConfig;
  styleSelectorToggle!: boolean;

  // Convenience getters that keep the template bindings working
  get navCollapsed(): boolean {
    return this.layout.sidebarCollapsed();
  }

  get navCollapsedMob(): boolean {
    return this.layout.mobileSidebarOpen();
  }

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

    // Sync body scroll-lock class with mobile sidebar state
    effect(() => {
      const isOpen = this.layout.mobileSidebarOpen();
      if (isOpen) {
        document.body.classList.add('sidebar-mobile-open');
      } else {
        document.body.classList.remove('sidebar-mobile-open');
      }
    });
  }

  ngOnDestroy(): void {
    document.body.classList.remove('sidebar-mobile-open');
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event): void {
    this.layout.updateWidth((event.target as Window).innerWidth);
  }

  // public method
  navMobClick() {
    this.layout.toggleMobileSidebar();
  }

  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeMenu();
    }
  }

  closeMenu() {
    this.layout.closeMobileSidebar();
  }
}
