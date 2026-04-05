import { Injectable, signal, computed } from '@angular/core';
import { MantisConfig } from 'src/app/app-config';

/**
 * Single source of truth for layout state (sidebar, mobile drawer).
 * Replaces scattered boolean flags in AdminLayout / NavBar / NavLeft.
 */
@Injectable({ providedIn: 'root' })
export class LayoutStateService {
  private readonly MOBILE_BREAKPOINT = 1025;

  /** Desktop sidebar collapsed (icon-only or hidden) */
  readonly sidebarCollapsed = signal<boolean>(MantisConfig.isCollapseMenu);

  /** Mobile sidebar drawer open */
  readonly mobileSidebarOpen = signal<boolean>(false);

  /** Tracks current viewport width */
  readonly windowWidth = signal<number>(typeof window !== 'undefined' ? window.innerWidth : 1200);

  /** True when viewport is below mobile breakpoint */
  readonly isMobile = computed(() => this.windowWidth() < this.MOBILE_BREAKPOINT);

  /** Toggle desktop sidebar collapse */
  toggleSidebar(): void {
    if (!this.isMobile()) {
      this.sidebarCollapsed.update(v => !v);
    }
  }

  /** Toggle mobile sidebar drawer */
  toggleMobileSidebar(): void {
    if (this.isMobile()) {
      this.mobileSidebarOpen.update(v => !v);
    }
  }

  /** Close mobile sidebar (e.g. overlay click, Escape) */
  closeMobileSidebar(): void {
    this.mobileSidebarOpen.set(false);
  }

  /** Call on window resize */
  updateWidth(width: number): void {
    this.windowWidth.set(width);
    // Auto-close mobile drawer when resizing to desktop
    if (width >= this.MOBILE_BREAKPOINT) {
      this.mobileSidebarOpen.set(false);
    }
  }
}
