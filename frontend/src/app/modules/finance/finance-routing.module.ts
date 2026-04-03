import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { authGuard, permissionGuard } from 'src/app/core/guards';
import { AdminLayout } from 'src/app/theme/layout/admin-layout/admin-layout.component';

const routes: Routes = [
  // ── GL – Chart of Accounts (single tree console) ──────────
  {
    path: 'gl/accounts',
    component: AdminLayout,
    children: [
      {
        path: '',
        redirectTo: 'tree',
        pathMatch: 'full'
      },
      {
        path: 'tree',
        loadComponent: () =>
          import('./gl/pages/accounts-tree/accounts-tree.component').then((c) => c.AccountsTreeComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_ACCOUNT_VIEW' }
      }
    ]
  },
  // ── GL – Accounting Rules ─────────────────────────────────
  {
    path: 'gl/rules',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./gl/pages/rules-search/rules-search.component').then((c) => c.RulesSearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_RULE_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./gl/pages/rules-form/rules-form.component').then((c) => c.RulesFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_RULE_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('./gl/pages/rules-form/rules-form.component').then((c) => c.RulesFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_RULE_UPDATE' }
      }
    ]
  },
  // ── GL – Journals ─────────────────────────────────────────
  {
    path: 'gl/journals',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./gl/pages/journals-search/journals-search.component').then((c) => c.JournalsSearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_JOURNAL_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./gl/pages/journals-form/journals-form.component').then((c) => c.JournalsFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_JOURNAL_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('./gl/pages/journals-form/journals-form.component').then((c) => c.JournalsFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_JOURNAL_UPDATE' }
      },
      {
        path: 'view/:id',
        loadComponent: () =>
          import('./gl/pages/journals-form/journals-form.component').then((c) => c.JournalsFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_JOURNAL_VIEW' }
      }
    ]
  },
  // ── GL – Postings ──────────────────────────────────────────
  {
    path: 'gl/postings',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./gl/pages/postings-search/postings-search.component').then((c) => c.PostingsSearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_POSTING_VIEW' }
      },
      {
        path: 'view/:id',
        loadComponent: () =>
          import('./gl/pages/posting-view/posting-view.component').then((c) => c.PostingViewComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_GL_POSTING_VIEW' }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FinanceRoutingModule {}
