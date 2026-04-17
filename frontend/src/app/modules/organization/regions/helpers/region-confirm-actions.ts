import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { RegionFacade } from '../facades/region.facade';
import { RegionListItemDto } from '../models/region.model';

export interface ConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: RegionFacade;
}

export function confirmDeactivateRegion(
  deps: ConfirmActionDeps,
  entity: RegionListItemDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_REGION_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'REGIONS.DEACTIVATE',
    messageKey: 'REGIONS.CONFIRM_DEACTIVATE',
    messageParams: { code: entity.regionCode },
    confirmKey: 'REGIONS.DEACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (!confirmed) return;
    deps.facade.deactivate(entity.regionPk, () => {
      deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
      onDone();
    });
  });
}
