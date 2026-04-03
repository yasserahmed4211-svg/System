import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { authGuard, permissionGuard } from 'src/app/core/guards';
import { AdminLayout } from 'src/app/theme/layout/admin-layout/admin-layout.component';
import { GuestLayouts } from 'src/app/theme/layout/guest-layout/guest-layout.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  // Guest/Public routes (no authentication required)
  {
    path: 'login',
    component: GuestLayouts,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./authentication/pages/auth-login/auth-login.component').then((c) => c.AuthLoginComponent)
      }
    ]
  },
  {
    path: 'register',
    component: GuestLayouts,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./authentication/pages/auth-register/auth-register.component').then((c) => c.AuthRegisterComponent)
      }
    ]
  },
  {
    path: 'forgot-password',
    component: GuestLayouts,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./authentication/pages/forgot-password/forgot-password.component').then((c) => c.ForgotPasswordComponent)
      }
    ]
  },
  // Redirect old sample-page path to the new dashboard
  {
    path: 'sample-page',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'users',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () => import('./user-management/pages/users-search/user-list.component').then((c) => c.UserListComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_USER_VIEW' }
      }
    ]
  },
  {
    path: 'pages-registry',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () => import('./pages-registry/pages/pages-search/pages-search.component').then((c) => c.PagesSearchComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_PAGE_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () => import('./pages-registry/pages/pages-form/pages-form.component').then((c) => c.PagesFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_PAGE_CREATE' }
      },
      {
        path: 'edit/:id',
        loadComponent: () => import('./pages-registry/pages/pages-form/pages-form.component').then((c) => c.PagesFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_PAGE_UPDATE' }
      }
    ]
  },
  {
    path: 'pages',
    redirectTo: 'pages-registry',
    pathMatch: 'full'
  },
  {
    path: 'role-access',
    component: AdminLayout,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./role-access/components/role-access-control/role-access-control.component').then((c) => c.RoleAccessControlComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_ROLE_VIEW' }
      },
      {
        path: 'create',
        loadComponent: () =>
          import('./role-access/pages/role-access-form/role-access-form.component').then((c) => c.RoleAccessFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_ROLE_VIEW' }
      },
      {
        path: 'edit/:roleId',
        loadComponent: () =>
          import('./role-access/pages/role-access-form/role-access-form.component').then((c) => c.RoleAccessFormComponent),
        canActivate: [authGuard, permissionGuard],
        data: { permission: 'PERM_ROLE_VIEW' }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SecurityRoutingModule {}
