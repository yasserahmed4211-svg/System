import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ErpPageHeaderComponent } from 'src/app/shared/components/erp-page-header';
import { PermissionService } from 'src/app/core/services/permission.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

interface QuickAccessCard {
  titleKey: string;
  descriptionKey: string;
  icon: string;
  route: string;
  permission?: string;
  colorClass: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, SharedModule, ErpPageHeaderComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DashboardComponent {
  private readonly permissionService = inject(PermissionService);
  private readonly authService = inject(AuthenticationService);

  /** Current logged-in user display name */
  readonly userName = computed(() => {
    const user = this.authService.currentUserValue;
    return user?.username ?? '';
  });

  /** All possible quick-access cards — filtered by user permissions */
  private readonly allCards: QuickAccessCard[] = [
    {
      titleKey: 'DASHBOARD.CARDS.USERS',
      descriptionKey: 'DASHBOARD.CARDS.USERS_DESC',
      icon: 'fas fa-users',
      route: '/security/users',
      permission: 'PERM_USER_VIEW',
      colorClass: 'card-users'
    },
    {
      titleKey: 'DASHBOARD.CARDS.ROLES',
      descriptionKey: 'DASHBOARD.CARDS.ROLES_DESC',
      icon: 'fas fa-user-shield',
      route: '/security/role-access',
      permission: 'PERM_ROLE_VIEW',
      colorClass: 'card-roles'
    },
    {
      titleKey: 'DASHBOARD.CARDS.PAGES',
      descriptionKey: 'DASHBOARD.CARDS.PAGES_DESC',
      icon: 'fas fa-file-alt',
      route: '/security/pages-registry',
      permission: 'PERM_PAGE_VIEW',
      colorClass: 'card-pages'
    },
    {
      titleKey: 'DASHBOARD.CARDS.MASTER_LOOKUPS',
      descriptionKey: 'DASHBOARD.CARDS.MASTER_LOOKUPS_DESC',
      icon: 'fas fa-database',
      route: '/master-data/master-lookups',
      permission: 'PERM_MASTER_LOOKUP_VIEW',
      colorClass: 'card-master'
    },
    {
      titleKey: 'DASHBOARD.CARDS.GL_ACCOUNTS',
      descriptionKey: 'DASHBOARD.CARDS.GL_ACCOUNTS_DESC',
      icon: 'fas fa-calculator',
      route: '/finance/gl/accounts',
      permission: 'PERM_GL_ACCOUNT_VIEW',
      colorClass: 'card-gl'
    },
    {
      titleKey: 'DASHBOARD.CARDS.GL_RULES',
      descriptionKey: 'DASHBOARD.CARDS.GL_RULES_DESC',
      icon: 'fas fa-balance-scale',
      route: '/finance/gl/rules',
      permission: 'PERM_GL_RULE_VIEW',
      colorClass: 'card-rules'
    }
  ];

  /** Cards visible to the current user based on their permissions */
  readonly visibleCards = computed(() =>
    this.allCards.filter((card) => !card.permission || this.permissionService.hasPermission(card.permission))
  );
}
