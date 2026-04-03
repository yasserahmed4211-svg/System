import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { NavigationItem } from 'src/app/theme/layout/admin-layout/navigation/navigation';
import { LanguageService } from 'src/app/core/services/language.service';
import { TranslateService } from '@ngx-translate/core';
import { ApiResponse } from 'src/app/shared/models/api-response.model';

export interface MenuItemDto {
  id: number;
  nameAr: string;
  nameEn: string;
  routePath?: string;
  parentId?: number | null;
  permCode?: string;
  module?: string;
  displayOrder: number;
  icon?: string;
  isActive: boolean;
  description?: string;
  children?: MenuItemDto[];
}

/**
 * @deprecated Use ApiResponse<MenuItemDto[]> from shared models instead.
 */
export interface MenuApiResponse extends ApiResponse<MenuItemDto[]> {}

/**
 * Icon mapping from legacy/database icon names to Ant Design icon names
 * This ensures compatibility between database-stored icons and Ant Design Angular library
 */
const ICON_MAPPING: Record<string, string> = {
  // User related
  'users': 'team',
  'users-o': 'team',
  'user': 'user',
  'user-o': 'user',
  'profile': 'user',
  
  // Menu/Navigation
  'menu': 'menu',
  'menu-o': 'menu',
  'bars': 'menu',
  
  // Security/Permissions
  'permissions': 'safety',
  'permission': 'safety',
  'security': 'safety',
  'shield': 'safety',
  'lock': 'lock',
  'unlock': 'unlock',
  'key': 'key',
  'role-access': 'safety',
  'role': 'team',
  'roles': 'team',
  
  // Files/Documents
  'file': 'file',
  'file-o': 'file',
  'files': 'folder',
  'folder': 'folder',
  'document': 'file-text',
  'file-text': 'file-text',
  
  // Settings
  'settings': 'setting',
  'setting': 'setting',
  'cog': 'setting',
  'gear': 'setting',
  
  // Money/Finance
  'money': 'dollar',
  'dollar': 'dollar',
  'bank': 'bank',
  'wallet': 'wallet',
  'credit-card': 'credit-card',
  
  // Dashboard/Analytics
  'dashboard': 'dashboard',
  'chart': 'bar-chart',
  'analytics': 'line-chart',
  'pie-chart': 'pie-chart',
  
  // General
  'home': 'home',
  'search': 'search',
  'plus': 'plus',
  'minus': 'minus',
  'edit': 'edit',
  'delete': 'delete',
  'trash': 'delete',
  'save': 'save',
  'check': 'check',
  'close': 'close',
  'info': 'info-circle',
  'warning': 'warning',
  'error': 'close-circle',
  'success': 'check-circle',
  'question': 'question-circle',
  
  // Business
  'company': 'bank',
  'building': 'bank',
  'shop': 'shop',
  'store': 'shop',
  'customer': 'user',
  'customers': 'team',
  'employee': 'user',
  'employees': 'team',
  
  // Tools
  'tool': 'tool',
  'wrench': 'tool',
  'hammer': 'tool',
  
  // Communication
  'mail': 'mail',
  'email': 'mail',
  'phone': 'phone',
  'message': 'message',
  'comment': 'comment',
  'notification': 'bell',
  'bell': 'bell',
  
  // Calendar/Time
  'calendar': 'calendar',
  'clock': 'clock-circle',
  'time': 'clock-circle',
  'history': 'history',
  
  // Navigation
  'arrow-right': 'arrow-right',
  'arrow-left': 'arrow-left',
  'arrow-up': 'arrow-up',
  'arrow-down': 'arrow-down',
  'right': 'right',
  'left': 'left',
  'up': 'up',
  'down': 'down',
  
  // Other
  'feather': 'file-text',
  'star': 'star',
  'heart': 'heart',
  'tag': 'tag',
  'tags': 'tags',
  'list': 'unordered-list',
  'table': 'table',
  'database': 'database',
  'cloud': 'cloud',
  'link': 'link',
  'globe': 'global',
  'world': 'global',
  'print': 'printer',
  'printer': 'printer',
  'book': 'book',
  'copy': 'copy',
  'paste': 'snippets',
  'filter': 'filter',
  'sort': 'sort-ascending',
  'export': 'export',
  'import': 'import',
  'download': 'download',
  'upload': 'upload',
  'refresh': 'reload',
  'reload': 'reload',
  'sync': 'sync',
  'eye': 'eye',
  'eye-invisible': 'eye-invisible',
  'code': 'code',
  'api': 'api',
  'appstore': 'appstore',
  'layout': 'layout',
  'form': 'form'
};

/**
 * Module icon mapping — maps MODULE codes to Ant Design icon names
 */
const MODULE_ICON_MAPPING: Record<string, string> = {
  'SECURITY': 'safety',
  'FINANCE': 'bank',
  'HR': 'team',
  'MASTER_DATA': 'database',
  'MASTERDATA': 'database',
  'REPORTS': 'pie-chart',
  'SETTINGS': 'setting',
  'SYSTEM': 'tool',
};

@Injectable({ providedIn: 'root' })
export class MenuService {
  private http = inject(HttpClient);
  private languageService = inject(LanguageService);
  private translate = inject(TranslateService);

  /**
   * Maps legacy icon name to Ant Design icon name
   * @param iconName - Icon name from database or API
   * @returns Ant Design compatible icon name
   */
  private mapIconName(iconName: string | undefined): string {
    const fallback = 'file-text';
    if (!iconName) {
      return fallback;
    }

    const normalized = iconName.toLowerCase().trim();
    if (!normalized) {
      return fallback;
    }

    // Treat DB as untrusted input.
    // Legacy formats like "ti ti-list-search" (spaces) or "ti-..." must never be passed through.
    if (normalized.includes(' ') || normalized.includes('ti-')) {
      const match = normalized.match(/\bti-([a-z0-9-]+)\b/);
      const legacyKey = match?.[1] || normalized.split(/\s+/).filter(Boolean).at(-1)?.replace(/^ti-/, '') || '';
      return ICON_MAPPING[legacyKey] || ICON_MAPPING[legacyKey.replace(/-/g, '')] || fallback;
    }

    // Disallow namespaced icons (would trigger dynamic loading).
    if (normalized.includes(':')) {
      return fallback;
    }

    // Strip theme suffix if stored.
    const baseName = normalized.replace(/-(fill|twotone|o)$/, '');

    // Check direct mapping first.
    if (ICON_MAPPING[baseName]) {
      return ICON_MAPPING[baseName];
    }

    // Only allow safe characters; otherwise fallback.
    if (!/^[a-z0-9-]+$/.test(baseName)) {
      return fallback;
    }

    // Return normalized Ant icon name (may still be validated at render time).
    return baseName;
  }

  /**
   * Get localized title based on current language
   */
  private getLocalizedTitle(item: MenuItemDto): string {
    return this.languageService.getLocalizedName(item.nameAr, item.nameEn);
  }
  /**
   * Fetches user menu from the backend based on user's permissions
   * Endpoint: GET /api/menu/user-menu
   */
  getUserMenu(): Observable<NavigationItem[]> {
    return this.http.get<MenuApiResponse>(`${environment.authApiUrl}/api/menu/user-menu`).pipe(
      map((response) => {
        if (!response.success || !response.data) {
          return [];
        }
        return this.transformMenuItems(response.data);
      }),
      catchError(() => {
        return of([]);
      })
    );
  }

  /**
   * Transforms backend MenuItemDto to NavigationItem format
   */
  private transformMenuItems(menuItems: MenuItemDto[]): NavigationItem[] {
    return menuItems
      .filter((item) => item.isActive)
      .map((item) => this.mapToNavigationItem(item))
      .sort((a, b) => {
        // Sort by displayOrder if available - stored during mapping
        const orderA = a.displayOrder ?? 0;
        const orderB = b.displayOrder ?? 0;
        return orderA - orderB;
      });
  }

  /**
   * Maps a single MenuItemDto to NavigationItem
   */
  private mapToNavigationItem(item: MenuItemDto): NavigationItem {
    const hasChildren = item.children && item.children.length > 0;
    
    const navigationItem: NavigationItem = {
      id: `menu-${item.id}`,
      title: this.getLocalizedTitle(item), // Use localized title based on current language
      type: hasChildren ? 'collapse' : 'item',
      icon: this.mapIconName(item.icon), // Map icon to Ant Design format
      url: hasChildren ? undefined : item.routePath,
      classes: hasChildren ? 'nav-item' : 'nav-item',
      breadcrumbs: true,
      displayOrder: item.displayOrder,
      module: item.module || undefined
    };
    
    // Add description if available
    if (item.description) {
      navigationItem.description = item.description;
    }
    
    // Add permission if available
    if (item.permCode) {
      navigationItem.role = [item.permCode];
    }

    // Recursively transform children
    if (hasChildren) {
      navigationItem.children = this.transformMenuItems(item.children!);
    }

    return navigationItem;
  }

  /**
   * Groups menu items by MODULE. Falls back to single "Navigation" group
   * if no items have a module set (backward compatible).
   */
  groupMenuItems(menuItems: NavigationItem[]): NavigationItem[] {
    if (menuItems.length === 0) {
      return [];
    }

    // Check if ANY item has a module — if not, fall back to flat group
    const hasModules = menuItems.some(item => !!item.module);
    if (!hasModules) {
      return [
        {
          id: 'navigation',
          title: 'Navigation',
          type: 'group',
          icon: 'icon-navigation',
          children: menuItems
        }
      ];
    }

    // Group items by module
    const moduleMap = new Map<string, NavigationItem[]>();
    const noModuleItems: NavigationItem[] = [];

    for (const item of menuItems) {
      if (item.module) {
        const key = item.module.toUpperCase();
        if (!moduleMap.has(key)) {
          moduleMap.set(key, []);
        }
        moduleMap.get(key)!.push(item);
      } else {
        noModuleItems.push(item);
      }
    }

    // Build group NavigationItems sorted by the minimum displayOrder in each module
    const groups: NavigationItem[] = [];

    // Items without a module go into a "General" group first
    if (noModuleItems.length > 0) {
      groups.push({
        id: 'module-general',
        title: this.getModuleDisplayName('GENERAL'),
        type: 'group',
        icon: 'icon-navigation',
        children: noModuleItems
      });
    }

    // Sort modules by the lowest displayOrder among their items
    const sortedModules = [...moduleMap.entries()].sort((a, b) => {
      const minA = Math.min(...a[1].map(i => i.displayOrder ?? 999));
      const minB = Math.min(...b[1].map(i => i.displayOrder ?? 999));
      return minA - minB;
    });

    for (const [moduleCode, items] of sortedModules) {
      groups.push({
        id: `module-${moduleCode.toLowerCase()}`,
        title: this.getModuleDisplayName(moduleCode),
        type: 'group',
        icon: MODULE_ICON_MAPPING[moduleCode] || 'appstore',
        children: items
      });
    }

    return groups;
  }

  /**
   * Returns the localized display name for a module code.
   * Tries translation key NAVIGATION.MODULE.<CODE>, falls back to formatted code.
   */
  private getModuleDisplayName(moduleCode: string): string {
    const key = `NAVIGATION.MODULE.${moduleCode}`;
    const translated = this.translate.instant(key);
    // If translation key was not found, instant() returns the key itself
    if (translated !== key) {
      return translated;
    }
    // Fallback: format code nicely (MASTER_DATA → Master Data)
    return moduleCode
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  }
}
