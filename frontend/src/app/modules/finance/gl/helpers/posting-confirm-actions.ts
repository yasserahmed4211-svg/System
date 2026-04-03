import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { PostingFacade } from '../facades/posting.facade';
import { AccPostingMstDto } from '../models/posting.model';
import { PostingJournalPreviewModalComponent } from '../components/posting-journal-preview-modal/posting-journal-preview-modal.component';

export interface PostingConfirmActionDeps {
  modal: NgbModal;
  notify: ErpNotificationService;
  auth: AuthenticationService;
  facade: PostingFacade;
}

// ── Generate Journal (with Preview Modal) ───────────────────

export function confirmGenerateJournal(
  deps: PostingConfirmActionDeps,
  posting: AccPostingMstDto,
  onDone: () => void
): void {
  if (!deps.auth.hasPermission('PERM_GL_POSTING_CREATE')) {
    deps.notify.warning('MESSAGES.NO_PERMISSION');
    return;
  }

  // Step 1: Fetch journal preview from backend
  deps.facade.previewJournal(posting.postingId, (preview) => {
    // Step 2: Open preview modal with the preview data
    const modalRef = deps.modal.open(PostingJournalPreviewModalComponent, {
      size: 'lg',
      centered: true,
      backdrop: 'static',
      keyboard: false
    });

    modalRef.componentInstance.posting = posting;
    modalRef.componentInstance.preview = preview;

    // Step 3: On confirm → generate journal
    modalRef.closed.subscribe((result: string) => {
      if (result === 'confirm') {
        deps.facade.generateJournal(posting.postingId, (response) => {
          deps.notify.success('GL.GENERATE_JOURNAL_SUCCESS');
          onDone();
        });
      }
    });
  });
}
