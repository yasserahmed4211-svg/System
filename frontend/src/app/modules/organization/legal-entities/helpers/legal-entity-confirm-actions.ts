import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { LegalEntityFacade } from '../facades/legal-entity.facade';
import { LegalEntityListItemDto } from '../models/legal-entity.model';

export interface ConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: LegalEntityFacade;
}

export function confirmDeactivateLegalEntity(
  deps: ConfirmActionDeps,
  entity: LegalEntityListItemDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_LEGAL_ENTITY_UPDATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'LEGAL_ENTITIES.DEACTIVATE',
    messageKey: 'LEGAL_ENTITIES.CONFIRM_DEACTIVATE',
    messageParams: { code: entity.legalEntityCode },
    confirmKey: 'LEGAL_ENTITIES.DEACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (!confirmed) return;
    deps.facade.deactivate(entity.legalEntityPk, () => {
      deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
      onDone();
    });
  });
}
