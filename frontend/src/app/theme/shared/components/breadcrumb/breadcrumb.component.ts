// Angular Import
import { Component, Input, input, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterModule, Event } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// project import
import { SharedModule } from '../../shared.module';
import { NavigationItem, NavigationItems } from 'src/app/theme/layout/admin-layout/navigation/navigation';
import { DASHBOARD_PATH } from 'src/app/app-config';

// icons
import { IconService } from '@ant-design/icons-angular';
import { GlobalOutline, NodeExpandOutline } from '@ant-design/icons-angular/icons';

interface titleType {
  url: string | boolean | undefined;
  title: string;
  breadcrumbs: unknown;
  type: string;
  link?: string | undefined;
  description?: string | undefined;
  path?: string | undefined;
}

@Component({
  selector: 'app-breadcrumb',
  imports: [CommonModule, RouterModule, SharedModule],
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.scss']
})
export class BreadcrumbComponent {
  private route = inject(Router);
  private titleService = inject(Title);
  private iconService = inject(IconService);
  private destroyRef = inject(DestroyRef);

  // public props
  @Input() type: string;
  readonly dashboard = input(true);

  dashboard_link = DASHBOARD_PATH;

  navigations: NavigationItem[];
  breadcrumbList: Array<string> = [];
  // eslint-disable-next-line
  navigationList!: any;

  // constructor
  constructor() {
    this.navigations = NavigationItems;
    this.type = 'theme1';
    this.setBreadcrumb();
    this.iconService.addIcon(...[GlobalOutline, NodeExpandOutline]);
  }

  // public method
  setBreadcrumb() {
    this.route.events
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((router: Event) => {
      if (router instanceof NavigationEnd) {
        const activeLink = router.url;
        const breadcrumbList = this.filterNavigation(this.navigations, activeLink);
        this.navigationList = breadcrumbList.splice(-2);
        const title = breadcrumbList[breadcrumbList.length - 1]?.title || 'Welcome';
        this.titleService.setTitle(title + ' | Mantis  Angular Admin Template');
      }
    });
  }

  filterNavigation(navItems: NavigationItem[], activeLink: string): titleType[] {
    for (const navItem of navItems) {
      if (navItem.type === 'item' && 'url' in navItem && navItem.url === activeLink) {
        return [
          {
            url: 'url' in navItem ? navItem.url : false,
            title: navItem.title,
            link: navItem.link,
            description: navItem.description,
            path: navItem.path,
            breadcrumbs: 'breadcrumbs' in navItem ? navItem.breadcrumbs : true,
            type: navItem.type
          }
        ];
      }
      if ((navItem.type === 'group' || navItem.type === 'collapse') && 'children' in navItem) {
        const breadcrumbList = this.filterNavigation(navItem.children!, activeLink);
        if (breadcrumbList.length > 0) {
          breadcrumbList.unshift({
            url: 'url' in navItem ? navItem.url : false,
            title: navItem.title,
            link: navItem.link,
            path: navItem.path,
            description: navItem.description,
            breadcrumbs: 'breadcrumbs' in navItem ? navItem.breadcrumbs : true,
            type: navItem.type
          });
          return breadcrumbList;
        }
      }
    }
    return [];
  }
}
