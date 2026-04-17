import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { RoleAccessFacade } from '../facades/role-access.facade';
import { RoleDto } from '../models/role-access.model';

export interface RoleConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: RoleAccessFacade;
}

// ── Toggle Role Active (used in role-access-control) ──

export function confirmToggleRoleActive(
  deps: RoleConfirmActionDeps,
  role: RoleDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_ROLE_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  const titleKey = role.active ? 'COMMON.DEACTIVATE' : 'COMMON.ACTIVATE';
  const messageKey = role.active ? 'MESSAGES.CONFIRM_ACTION' : 'MESSAGES.CONFIRM_ACTIVATE';

  deps.dialog
    .confirm({
      titleKey,
      messageKey,
      messageParams: { name: role.roleName },
      confirmKey: titleKey,
      cancelKey: 'COMMON.CANCEL',
      type: 'warning'
    })
    .then((confirmed) => {
      if (!confirmed) return;
      deps.facade.toggleRoleActive(role, !role.active, () => {
        const successKey = role.active ? 'MESSAGES.DEACTIVATE_SUCCESS' : 'MESSAGES.ACTIVATE_SUCCESS';
        deps.notify.success(successKey);
        onDone();
      });
    });
}

// ── Delete Role (used in role-access-control) ──

export function confirmDeleteRole(
  deps: RoleConfirmActionDeps,
  role: RoleDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_ROLE_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog
    .confirm({
      titleKey: 'ROLE_ACCESS.DELETE_CONFIRM_TITLE',
      messageKey: 'ROLE_ACCESS.DELETE_CONFIRM_MESSAGE',
      messageParams: { name: role.roleName },
      confirmKey: 'COMMON.DELETE',
      cancelKey: 'COMMON.CANCEL',
      type: 'danger'
    })
    .then((confirmed) => {
      if (!confirmed) return;
      deps.facade.deleteRole(role.id, () => {
        deps.notify.success('MESSAGES.DELETE_SUCCESS');
        onDone();
      });
    });
}

// ── Remove Page from Role (used in role-access-form) ──

export function confirmRemoveRolePage(
  deps: RoleConfirmActionDeps,
  roleId: number,
  pageCode: string,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_ROLE_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog
    .confirm({
      titleKey: 'ROLE_ACCESS.REMOVE_PAGE',
      messageKey: 'ROLE_ACCESS.REMOVE_PAGE',
      messageParams: { pageCode },
      confirmKey: 'COMMON.DELETE',
      cancelKey: 'COMMON.CANCEL',
      type: 'warning'
    })
    .then((confirmed) => {
      if (!confirmed) return;
      deps.facade.removeRolePage(roleId, pageCode, () => {
        deps.notify.success('MESSAGES.DELETE_SUCCESS');
      });
    });
}
