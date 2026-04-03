import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { MasterLookupFacade } from '../facades/master-lookup.facade';
import { MasterLookupDto, LookupDetailDto } from '../models/master-lookup.model';

export interface ConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: MasterLookupFacade;
}

// ── Master Lookup Actions (used in Search Page A) ──

export function confirmToggleLookupActive(deps: ConfirmActionDeps, lookup: MasterLookupDto, onDone: () => void): void {
  if (!deps.auth.hasPermission('PERM_MASTER_LOOKUP_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }
  const deactivating = lookup.isActive;
  deps.dialog.confirm({
    titleKey: deactivating ? 'MASTER_LOOKUPS.DEACTIVATE' : 'MASTER_LOOKUPS.ACTIVATE',
    messageKey: deactivating ? 'MASTER_LOOKUPS.CONFIRM_DEACTIVATE' : 'MASTER_LOOKUPS.CONFIRM_ACTIVATE',
    messageParams: { lookupKey: lookup.lookupKey },
    confirmKey: deactivating ? 'MASTER_LOOKUPS.DEACTIVATE' : 'MASTER_LOOKUPS.ACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (!confirmed) return;
    const newActive = !lookup.isActive;
    deps.facade.toggleMasterLookupActive(lookup.id, newActive, () => {
      deps.notify.success(newActive ? 'MESSAGES.ACTIVATE_SUCCESS' : 'MESSAGES.DEACTIVATE_SUCCESS');
      onDone();
    });
  });
}

export function confirmDeleteLookup(deps: ConfirmActionDeps, lookup: MasterLookupDto, onDone: () => void): void {
  if (!deps.auth.hasPermission('PERM_MASTER_LOOKUP_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }
  deps.facade.getUsageInfo(lookup.id, (usage) => {
    if (!usage.canDelete) {
      deps.notify.warning('MASTER_LOOKUPS.CANNOT_DELETE_HAS_DETAILS');
      return;
    }
    deps.dialog.confirm({
      titleKey: 'MASTER_LOOKUPS.DELETE',
      messageKey: 'MASTER_LOOKUPS.CONFIRM_DELETE',
      messageParams: { lookupKey: lookup.lookupKey },
      confirmKey: 'COMMON.DELETE',
      cancelKey: 'COMMON.CANCEL',
      type: 'danger'
    }).then((confirmed) => {
      if (!confirmed) return;
      deps.facade.deleteMasterLookup(lookup.id, () => {
        deps.notify.success('MESSAGES.DELETE_SUCCESS');
        onDone();
      });
    });
  });
}

// ── Lookup Detail Actions (used in Entry Page B) ──

export function confirmToggleDetailActive(
  deps: ConfirmActionDeps, detail: LookupDetailDto, masterLookupId: number
): void {
  if (!deps.auth.hasPermission('PERM_MASTER_LOOKUP_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }
  const deactivating = detail.isActive;
  deps.dialog.confirm({
    titleKey: deactivating ? 'MASTER_LOOKUPS.DEACTIVATE' : 'MASTER_LOOKUPS.ACTIVATE',
    messageKey: deactivating ? 'MASTER_LOOKUPS.CONFIRM_DEACTIVATE_DETAIL' : 'MASTER_LOOKUPS.CONFIRM_ACTIVATE_DETAIL',
    messageParams: { code: detail.code },
    confirmKey: deactivating ? 'MASTER_LOOKUPS.DEACTIVATE' : 'MASTER_LOOKUPS.ACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (!confirmed) return;
    const newActive = !detail.isActive;
    deps.facade.toggleLookupDetailActive(detail.id, newActive, masterLookupId, () => {
      deps.notify.success(newActive ? 'MESSAGES.ACTIVATE_SUCCESS' : 'MESSAGES.DEACTIVATE_SUCCESS');
    });
  });
}

export function confirmDeleteDetail(
  deps: ConfirmActionDeps, detail: LookupDetailDto, masterLookupId: number
): void {
  if (!deps.auth.hasPermission('PERM_MASTER_LOOKUP_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }
  deps.dialog.confirm({
    titleKey: 'MASTER_LOOKUPS.DELETE_DETAIL',
    messageKey: 'MASTER_LOOKUPS.CONFIRM_DELETE_DETAIL',
    messageParams: { code: detail.code },
    confirmKey: 'COMMON.DELETE',
    cancelKey: 'COMMON.CANCEL',
    type: 'danger'
  }).then((confirmed) => {
    if (!confirmed) return;
    deps.facade.deleteLookupDetail(detail.id, masterLookupId, () => {
      deps.notify.success('MESSAGES.DELETE_SUCCESS');
    });
  });
}
