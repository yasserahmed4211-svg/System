import { Pipe, PipeTransform, inject } from '@angular/core';
import { IconService } from '@ant-design/icons-angular';

type AntIconTheme = 'outline' | 'fill' | 'twotone';

@Pipe({
  name: 'safeAntIcon',
  standalone: true,
  pure: true
})
export class SafeAntIconPipe implements PipeTransform {
  private readonly iconService = inject(IconService);

  // Must be registered (NavContentComponent currently registers this).
  private readonly fallbackIcon = 'file-text';

  // Minimal, defensive legacy mapping. We intentionally keep this small and focused
  // on stability (no icon errors) rather than perfect visual parity.
  private readonly legacyToAnt: Record<string, string> = {
    // Common DB legacy examples (Tabler/Themify-like class names)
    'list-search': 'search',
    'list': 'unordered-list',
    'database': 'database',
    'alert-circle': 'warning',
    'info-circle': 'info-circle',
    'plus': 'plus',
    'refresh': 'reload',
    'edit': 'edit',
    'trash': 'delete',
    'filter': 'filter',
    'device-floppy': 'save',
    'copy': 'copy',
    'x': 'close',
    'lock': 'lock',
    'unlock': 'unlock',
    'user': 'user',
    'users': 'team',
    'menu': 'menu',
    'file': 'file'
  };

  transform(value: unknown, theme: AntIconTheme = 'outline'): string {
    const resolved = this.resolveToAntIconName(value);
    if (this.isRegistered(resolved, theme)) {
      return resolved;
    }
    return this.isRegistered(this.fallbackIcon, theme) ? this.fallbackIcon : resolved || this.fallbackIcon;
  }

  private resolveToAntIconName(value: unknown): string {
    if (typeof value !== 'string') {
      return this.fallbackIcon;
    }

    const trimmed = value.trim();
    if (!trimmed) {
      return this.fallbackIcon;
    }

    const normalized = trimmed.toLowerCase();

    // Treat DB as untrusted: disallow namespaces and suspicious characters.
    // Namespaced icons would trigger dynamic loading from assets.
    if (normalized.includes(':')) {
      return this.fallbackIcon;
    }

    // If icon contains spaces or ti- prefix, treat as legacy (e.g. "ti ti-list-search").
    if (normalized.includes(' ') || normalized.includes('ti-')) {
      const legacyKey = this.extractLegacyToken(normalized);
      return this.legacyToAnt[legacyKey] || this.fallbackIcon;
    }

    // Strip theme suffix if the DB stored it (avoid dev warnings about ignored theme).
    const baseName = this.stripThemeSuffix(normalized);

    // Only allow a safe, expected character set.
    if (!/^[a-z0-9-]+$/.test(baseName)) {
      return this.fallbackIcon;
    }

    return baseName;
  }

  private extractLegacyToken(normalized: string): string {
    // Examples:
    // - "ti ti-list-search" -> "list-search"
    // - "ti ti-database" -> "database"
    // - "ti-list" -> "list"
    const match = normalized.match(/\bti-([a-z0-9-]+)\b/);
    if (match?.[1]) {
      return match[1];
    }

    // Fallback: take last token and try to remove a leading "ti-" if present.
    const lastToken = normalized.split(/\s+/).filter(Boolean).at(-1) || '';
    return lastToken.startsWith('ti-') ? lastToken.slice(3) : lastToken;
  }

  private stripThemeSuffix(name: string): string {
    if (name.endsWith('-fill')) {
      return name.slice(0, -5);
    }
    if (name.endsWith('-twotone')) {
      return name.slice(0, -8);
    }
    if (name.endsWith('-o')) {
      return name.slice(0, -2);
    }
    return name;
  }

  private isRegistered(iconName: string, theme: AntIconTheme): boolean {
    const safeName = this.stripThemeSuffix(iconName);
    if (!safeName) {
      return false;
    }
    const cache = this.iconService.getCachedIcons();
    const key = this.buildCacheKey(safeName, theme);
    return cache.has(key);
  }

  private buildCacheKey(name: string, theme: AntIconTheme): string {
    switch (theme) {
      case 'fill':
        return `${name}-fill`;
      case 'twotone':
        return `${name}-twotone`;
      case 'outline':
      default:
        return `${name}-o`;
    }
  }
}
