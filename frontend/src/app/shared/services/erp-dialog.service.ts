import { Injectable, inject } from '@angular/core';
import { NgbModal, NgbModalOptions } from '@ng-bootstrap/ng-bootstrap';

/**
 * Options for the confirmation dialog.
 */
export interface ErpDialogOptions {
  /** Translation key for the dialog title */
  titleKey: string;
  /** Translation key for the dialog message */
  messageKey: string;
  /** Parameters for message translation interpolation */
  messageParams?: Record<string, unknown>;
  /** Translation key for the confirm button */
  confirmKey?: string;
  /** Translation key for the cancel button */
  cancelKey?: string;
  /** Visual type/severity of the dialog */
  type?: 'danger' | 'warning' | 'info';
}

/**
 * ErpDialogService
 * 
 * UI-level service for displaying confirmation dialogs.
 * Wraps NgBootstrap modal and the existing ConfirmDialogComponent.
 * Contains no business logic.
 * 
 * @requirement FE-REQ-SHARED-001
 * @task TASK-FE-SHARED-001
 */
@Injectable({
  providedIn: 'root'
})
export class ErpDialogService {
  private readonly modalService = inject(NgbModal);

  /**
   * Show a confirmation dialog.
   * 
   * @param options - Dialog configuration options
   * @returns Promise that resolves to true if confirmed, false if cancelled
   * 
   * @example
   * ```typescript
   * const confirmed = await this.dialogService.confirm({
   *   titleKey: 'COMMON.CONFIRM_DELETE',
   *   messageKey: 'COMMON.DELETE_CONFIRMATION',
   *   type: 'danger'
   * });
   * 
   * if (confirmed) {
   *   // proceed with deletion
   * }
   * ```
   */
  async confirm(options: ErpDialogOptions): Promise<boolean> {
    // Lazy load the dialog component to avoid circular dependencies
    const { ConfirmDialogComponent } = await import(
      'src/app/core/components/confirm-dialog/confirm-dialog.component'
    );

    const modalOptions: NgbModalOptions = {
      centered: true,
      backdrop: 'static',
      size: 'sm',
      keyboard: true
    };

    const modalRef = this.modalService.open(ConfirmDialogComponent, modalOptions);

    // Set dialog inputs
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
      // Modal was dismissed (cancel, backdrop click, escape key)
      return false;
    }
  }

  /**
   * Show a delete confirmation dialog with danger styling.
   * Convenience method for delete confirmations.
   * 
   * @param messageKey - Translation key for the message
   * @param messageParams - Optional parameters for message interpolation
   * @returns Promise that resolves to true if confirmed
   */
  async confirmDelete(
    messageKey = 'COMMON.DELETE_CONFIRMATION',
    messageParams?: Record<string, unknown>
  ): Promise<boolean> {
    return this.confirm({
      titleKey: 'COMMON.CONFIRM_DELETE',
      messageKey,
      messageParams,
      confirmKey: 'COMMON.DELETE',
      type: 'danger'
    });
  }

  /**
   * Show a discard changes confirmation dialog.
   * Convenience method for unsaved changes warnings.
   * 
   * @returns Promise that resolves to true if user confirms discarding changes
   */
  async confirmDiscard(): Promise<boolean> {
    return this.confirm({
      titleKey: 'COMMON.UNSAVED_CHANGES',
      messageKey: 'COMMON.DISCARD_CHANGES_MESSAGE',
      confirmKey: 'COMMON.DISCARD',
      type: 'warning'
    });
  }
}
