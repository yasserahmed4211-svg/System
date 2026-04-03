import { Injectable, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';

export interface ConfirmDialogOptions {
  titleKey: string;
  messageKey: string;
  messageParams?: Record<string, unknown>;
  confirmKey?: string;
  cancelKey?: string;
  type?: 'danger' | 'warning' | 'info';
}

/**
 * Service for displaying confirmation dialogs using NgBootstrap modal.
 */
@Injectable({
  providedIn: 'root'
})
export class ConfirmDialogService {
  private readonly modalService = inject(NgbModal);
  private readonly translate = inject(TranslateService);

  /**
   * Show a confirmation dialog.
   * @returns Promise<boolean> - true if confirmed, false if cancelled
   */
  async confirm(options: ConfirmDialogOptions): Promise<boolean> {
    const { ConfirmDialogComponent } = await import('../components/confirm-dialog/confirm-dialog.component');
    
    const modalRef = this.modalService.open(ConfirmDialogComponent, {
      centered: true,
      backdrop: 'static',
      size: 'sm'
    });

    modalRef.componentInstance.titleKey = options.titleKey;
    modalRef.componentInstance.messageKey = options.messageKey;
    modalRef.componentInstance.messageParams = options.messageParams || {};
    modalRef.componentInstance.confirmKey = options.confirmKey || 'COMMON.CONFIRM';
    modalRef.componentInstance.cancelKey = options.cancelKey || 'COMMON.CANCEL';
    modalRef.componentInstance.type = options.type || 'warning';

    try {
      await modalRef.result;
      return true;
    } catch {
      return false;
    }
  }
}
