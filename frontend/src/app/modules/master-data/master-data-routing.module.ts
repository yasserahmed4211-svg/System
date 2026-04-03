import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { authGuard, permissionGuard } from 'src/app/core/guards';
import { AdminLayout } from 'src/app/theme/layout/admin-layout/admin-layout.component';

const routes: Routes = [
  // ── Master Lookups ────────────────────────────────────────
  {
    path: 'master-lookups',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./master-lookups/pages/master-lookup-search/master-lookup-search.component').then((c) => c.MasterLookupSearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_MASTER_LOOKUP_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./master-lookups/pages/master-lookup-entry/master-lookup-entry.component').then((c) => c.MasterLookupEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_MASTER_LOOKUP_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('./master-lookups/pages/master-lookup-entry/master-lookup-entry.component').then((c) => c.MasterLookupEntryComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_MASTER_LOOKUP_UPDATE' }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MasterDataRoutingModule {}
