import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { PagesFacade } from '../facades/pages.facade';
import { PageDto } from '../models/page.model';

export interface PageConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: PagesFacade;
}

// ── Toggle Page Active (used in pages-search) ──

export function confirmTogglePageActive(
  deps: PageConfirmActionDeps,
  page: PageDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_PAGE_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  const action = page.active ? 'PAGES.CONFIRM_DEACTIVATE_MESSAGE' : 'PAGES.CONFIRM_ACTIVATE_MESSAGE';

  deps.dialog.confirm({
    titleKey: page.active ? 'PAGES.DEACTIVATE' : 'PAGES.ACTIVATE',
    messageKey: action,
    messageParams: { pageCode: page.pageCode },
    confirmKey: page.active ? 'PAGES.DEACTIVATE' : 'PAGES.ACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.togglePageActive(page, () => {
        const successMsg = page.active ? 'MESSAGES.DEACTIVATE_SUCCESS' : 'MESSAGES.ACTIVATE_SUCCESS';
        deps.notify.success(successMsg);
        onDone();
      });
    }
  });
}
