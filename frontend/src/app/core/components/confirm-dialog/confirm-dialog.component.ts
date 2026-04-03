import { Component, ChangeDetectionStrategy, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

/**
 * Confirm Dialog Component
 * 
 * Reusable confirmation modal with customizable title, message, and buttons.
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule
  ],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">{{ titleKey | translate }}</h5>
      <button
        type="button"
        class="btn-close"
        aria-label="Close"
        (click)="onCancel()"
      ></button>
    </div>
    <div class="modal-body">
      <div class="confirm-content" [ngClass]="'type-' + type">
        <i class="confirm-icon" [ngClass]="iconClass"></i>
        <p class="confirm-message">{{ messageKey | translate: messageParams }}</p>
      </div>
    </div>
    <div class="modal-footer">
      <button
        type="button"
        class="btn btn-secondary"
        (click)="onCancel()"
      >
        {{ cancelKey | translate }}
      </button>
      <button
        type="button"
        class="btn"
        [ngClass]="confirmBtnClass"
        (click)="onConfirm()"
      >
        {{ confirmKey | translate }}
      </button>
    </div>
  `,
  styles: [`
    .modal-header {
      border-bottom: 1px solid var(--surface-border);
    }
    
    .modal-body {
      padding: 1.5rem;
    }
    
    .modal-footer {
      border-top: 1px solid var(--surface-border);
      padding: 1rem 1.5rem;
    }
    
    .confirm-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
    }
    
    .confirm-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }
    
    .type-danger .confirm-icon {
      color: var(--red-500, #dc3545);
    }
    
    .type-warning .confirm-icon {
      color: var(--yellow-500, #ffc107);
    }
    
    .type-info .confirm-icon {
      color: var(--blue-500, #0d6efd);
    }
    
    .confirm-message {
      font-size: 1rem;
      margin: 0;
      color: var(--text-color);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfirmDialogComponent {
  private readonly activeModal = inject(NgbActiveModal);

  @Input() titleKey = 'COMMON.CONFIRM';
  @Input() messageKey = '';
  @Input() messageParams: Record<string, unknown> = {};
  @Input() confirmKey = 'COMMON.CONFIRM';
  @Input() cancelKey = 'COMMON.CANCEL';
  @Input() type: 'danger' | 'warning' | 'info' = 'warning';

  get iconClass(): string {
    switch (this.type) {
      case 'danger':
        return 'fas fa-exclamation-triangle';
      case 'info':
        return 'fas fa-info-circle';
      default:
        return 'fas fa-question-circle';
    }
  }

  get confirmBtnClass(): string {
    switch (this.type) {
      case 'danger':
        return 'btn-danger';
      case 'info':
        return 'btn-info';
      default:
        return 'btn-warning';
    }
  }

  onCancel(): void {
    this.activeModal.dismiss();
  }

  onConfirm(): void {
    this.activeModal.close(true);
  }
}
