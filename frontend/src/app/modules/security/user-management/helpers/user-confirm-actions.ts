import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { UserDto } from '../models/user.model';

export interface UserConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
}

// ── User Delete Action (used in user-list) ──

export function confirmDeleteUser(
  deps: UserConfirmActionDeps,
  user: UserDto,
  onConfirmed: () => void
): void {
  if (!deps.auth.hasPermission('PERM_USER_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirmDelete(
    'USERS.CONFIRM_DELETE',
    { username: user.username }
  ).then((confirmed: boolean) => {
    if (confirmed) {
      onConfirmed();
    }
  });
}
