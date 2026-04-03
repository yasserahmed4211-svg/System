import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { GlFacade } from '../facades/gl.facade';
import { AccountChartDto, AccRuleHdrDto } from '../models/gl.model';

export interface GlConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: GlFacade;
}

// ── Account Actions (used in accounts-search) ──

export function confirmDeactivateAccount(
  deps: GlConfirmActionDeps,
  account: AccountChartDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_GL_ACCOUNT_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  // Enhanced warning for root / parent-level accounts
  const isRootOrParent = (account.level ?? 0) === 0 || !account.accountChartFk;
  const messageKey = isRootOrParent
    ? 'GL.CONFIRM_DEACTIVATE_PARENT_ACCOUNT'
    : 'GL.CONFIRM_DEACTIVATE_ACCOUNT';

  deps.dialog.confirm({
    titleKey: 'COMMON.DEACTIVATE',
    messageKey,
    messageParams: { name: account.accountChartName, code: account.accountChartNo },
    confirmKey: 'COMMON.DEACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.deactivateAccount(account.accountChartPk, () => {
        deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
        onDone();
      });
    }
  });
}

// ── Rule Actions (used in rules-search) ──

export function confirmDeactivateRule(
  deps: GlConfirmActionDeps,
  rule: AccRuleHdrDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_GL_RULE_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'COMMON.DEACTIVATE',
    messageKey: 'GL.CONFIRM_DEACTIVATE_RULE',
    messageParams: { ruleId: rule.ruleId },
    confirmKey: 'COMMON.DEACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.deactivateRule(rule.ruleId, () => {
        deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
        onDone();
      });
    }
  });
}

// ── Form Parent-Change Confirmations (used in accounts-form) ──

export function confirmParentChange(
  deps: GlConfirmActionDeps,
  onConfirmed: () => void
): void {
  deps.dialog.confirm({
    titleKey: 'GL.PARENT_CHANGE_TITLE',
    messageKey: 'GL.PARENT_CHANGE_WARNING',
    confirmKey: 'COMMON.CONFIRM',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      onConfirmed();
    }
  });
}

export function confirmMakeRoot(
  deps: GlConfirmActionDeps,
  onConfirmed: () => void
): void {
  deps.dialog.confirm({
    titleKey: 'GL.PARENT_CHANGE_TITLE',
    messageKey: 'GL.MAKE_ROOT_WARNING',
    confirmKey: 'COMMON.CONFIRM',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      onConfirmed();
    }
  });
}
