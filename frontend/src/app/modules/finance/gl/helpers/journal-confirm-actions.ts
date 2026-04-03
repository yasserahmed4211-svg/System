import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { JournalFacade } from '../facades/journal.facade';
import { GlJournalHdrDto } from '../models/journal.model';

export interface JournalConfirmActionDeps {
  dialog: ErpDialogService;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: JournalFacade;
}

// ── Deactivate ──────────────────────────────────────────────

export function confirmDeactivateJournal(
  deps: JournalConfirmActionDeps,
  journal: GlJournalHdrDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_GL_JOURNAL_DELETE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'COMMON.DEACTIVATE',
    messageKey: 'GL.CONFIRM_DEACTIVATE_JOURNAL',
    messageParams: { journalNo: journal.journalNo },
    confirmKey: 'COMMON.DEACTIVATE',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.toggleActiveJournal(journal.id, false, () => {
        deps.notify.success('MESSAGES.DEACTIVATE_SUCCESS');
        onDone();
      });
    }
  });
}

// ── Approve ─────────────────────────────────────────────────

export function confirmApproveJournal(
  deps: JournalConfirmActionDeps,
  journal: GlJournalHdrDto,
  onDone: () => void
): void {
  if (!deps.auth.hasAnyPermission(['PERM_GL_JOURNAL_APPROVE', 'PERM_GL_JOURNAL_UPDATE'])) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'GL.APPROVE',
    messageKey: 'GL.CONFIRM_APPROVE_JOURNAL',
    messageParams: { journalNo: journal.journalNo },
    confirmKey: 'GL.APPROVE',
    cancelKey: 'COMMON.CANCEL',
    type: 'info'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.approveJournal(journal.id, () => {
        deps.notify.success('GL.APPROVE_SUCCESS');
        onDone();
      });
    }
  });
}

// ── Post ────────────────────────────────────────────────────

export function confirmPostJournal(
  deps: JournalConfirmActionDeps,
  journal: GlJournalHdrDto,
  onDone: () => void
): void {
  if (!deps.auth.hasAnyPermission(['PERM_GL_JOURNAL_POST', 'PERM_GL_JOURNAL_UPDATE'])) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'GL.POST',
    messageKey: 'GL.CONFIRM_POST_JOURNAL',
    messageParams: { journalNo: journal.journalNo },
    confirmKey: 'GL.POST',
    cancelKey: 'COMMON.CANCEL',
    type: 'warning'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.postJournal(journal.id, () => {
        deps.notify.success('GL.POST_SUCCESS');
        onDone();
      });
    }
  });
}

// ── Reverse ─────────────────────────────────────────────────

export function confirmReverseJournal(
  deps: JournalConfirmActionDeps,
  journal: GlJournalHdrDto,
  onDone: () => void
): void {
  if (!deps.auth.hasAnyPermission(['PERM_GL_JOURNAL_REVERSE', 'PERM_GL_JOURNAL_UPDATE'])) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'GL.REVERSE',
    messageKey: 'GL.CONFIRM_REVERSE_JOURNAL',
    messageParams: { journalNo: journal.journalNo },
    confirmKey: 'GL.REVERSE',
    cancelKey: 'COMMON.CANCEL',
    type: 'danger'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.reverseJournal(journal.id, () => {
        deps.notify.success('GL.REVERSE_SUCCESS');
        onDone();
      });
    }
  });
}

// ── Cancel ──────────────────────────────────────────────────

export function confirmCancelJournal(
  deps: JournalConfirmActionDeps,
  journal: GlJournalHdrDto,
  onDone: () => void
): void {
  if (!deps.auth.hasAnyPermission(['PERM_GL_JOURNAL_CANCEL', 'PERM_GL_JOURNAL_UPDATE'])) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  deps.dialog.confirm({
    titleKey: 'GL.CANCEL_JOURNAL',
    messageKey: 'GL.CONFIRM_CANCEL_JOURNAL',
    messageParams: { journalNo: journal.journalNo },
    confirmKey: 'GL.CANCEL_JOURNAL',
    cancelKey: 'COMMON.CANCEL',
    type: 'danger'
  }).then((confirmed) => {
    if (confirmed) {
      deps.facade.cancelJournal(journal.id, () => {
        deps.notify.success('GL.CANCEL_SUCCESS');
        onDone();
      });
    }
  });
}
