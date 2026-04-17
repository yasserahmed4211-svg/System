import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { BranchFacade } from '../facades/branch.facade';
import { BranchListItemDto, DepartmentDto } from '../models/branch.model';

export interface ConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: BranchFacade;
}

export function confirmDeactivateBranch(
  deps: ConfirmActionDeps,
  entity: BranchListItemDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_BRANCH_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'BRANCHES.DEACTIVATE',
    messageKey: 'BRANCHES.CONFIRM_DEACTIVATE',
    messageParams: { code: entity.branchCode },
    confirmKey: 'BRANCHES.DEACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (!confirmed) return;
    deps.facade.deactivate(entity.branchPk, () => {
      deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
      onDone();
    });
  });
}

export function confirmDeactivateDepartment(
  deps: ConfirmActionDeps,
  dept: DepartmentDto,
  onDone?: () => void
): void {
  if (!deps.auth.hasPermission('PERM_BRANCH_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'BRANCHES.DEACTIVATE_DEPT',
    messageKey: 'BRANCHES.CONFIRM_DEACTIVATE_DEPT',
    messageParams: { code: dept.departmentCode },
    confirmKey: 'BRANCHES.DEACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (!confirmed) return;
    deps.facade.deactivateDepartment(dept.departmentPk, () => {
      deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
      onDone?.();
    });
  });
}
