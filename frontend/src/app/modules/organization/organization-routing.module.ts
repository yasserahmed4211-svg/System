import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { authGuard, permissionGuard } from 'src/app/core/guards';
import { AdminLayout } from 'src/app/theme/layout/admin-layout/admin-layout.component';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'legal-entities'
  },

  // ── Legal Entities ─────────────────────────────────────────────────────────
  {
    path: 'legal-entities',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./legal-entities/pages/legal-entity-search/legal-entity-search.component')
            .then(c => c.LegalEntitySearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_LEGAL_ENTITY_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./legal-entities/pages/legal-entity-entry/legal-entity-entry.component')
            .then(c => c.LegalEntityEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_LEGAL_ENTITY_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('./legal-entities/pages/legal-entity-entry/legal-entity-entry.component')
            .then(c => c.LegalEntityEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_LEGAL_ENTITY_VIEW' }
      }
    ]
  },

  // ── Regions ────────────────────────────────────────────────────────────────
  {
    path: 'regions',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./regions/pages/region-search/region-search.component')
            .then(c => c.RegionSearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_REGION_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./regions/pages/region-entry/region-entry.component')
            .then(c => c.RegionEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_REGION_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('./regions/pages/region-entry/region-entry.component')
            .then(c => c.RegionEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_REGION_VIEW' }
      }
    ]
  },

  // ── Branches ───────────────────────────────────────────────────────────────
  {
    path: 'branches',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./branches/pages/branch-search/branch-search.component')
            .then(c => c.BranchSearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_BRANCH_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./branches/pages/branch-entry/branch-entry.component')
            .then(c => c.BranchEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_BRANCH_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('./branches/pages/branch-entry/branch-entry.component')
            .then(c => c.BranchEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_BRANCH_VIEW' }
      }
    ]
  }

];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class OrganizationRoutingModule {}
