import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

/**
 * ErpFormActionsComponent
 * 
 * Generic form action buttons (Save/Cancel) with loading and disabled states.
 * Entity-agnostic - contains no business logic or submit handling.
 * 
 * @requirement FE-REQ-SHARED-001
 * @task TASK-FE-SHARED-001
 */
@Component({
  selector: 'erp-form-actions',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule
  ],
  template: `
    <div class="erp-form-actions">
      <button
        *ngIf="showCancel"
        type="button"
        class="btn btn-secondary"
        [disabled]="loading"
        (click)="onCancel()"
      >
        {{ cancelKey | translate }}
      </button>
      
      <button
        *ngIf="showSave"
        type="button"
        class="btn btn-primary"
        [disabled]="loading || disabled"
        (click)="onSave()"
      >
        <span
          *ngIf="loading"
          class="spinner-border spinner-border-sm me-1"
          role="status"
          aria-hidden="true"
        ></span>
        {{ (loading ? loadingKey : saveKey) | translate }}
      </button>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
    
    .erp-form-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: var(--erp-spacing-sm, 0.75rem);
      padding-block-start: var(--erp-spacing-md, 1rem);
      margin-block-start: var(--erp-spacing-md, 1rem);
      border-block-start: 1px solid var(--erp-color-border, #dee2e6);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpFormActionsComponent {
  /** Translation key for the save button */
  @Input() saveKey = 'COMMON.SAVE';
  
  /** Translation key for the cancel button */
  @Input() cancelKey = 'COMMON.CANCEL';
  
  /** Translation key displayed while loading */
  @Input() loadingKey = 'COMMON.SAVING';
  
  /** Whether to show the save button */
  @Input() showSave = true;
  
  /** Whether to show the cancel button */
  @Input() showCancel = true;
  
  /** Whether the form is currently submitting */
  @Input() loading = false;
  
  /** Whether the save button should be disabled (e.g., invalid form) */
  @Input() disabled = false;
  
  /** Emitted when the save button is clicked */
  @Output() saveClicked = new EventEmitter<void>();
  
  /** Emitted when the cancel button is clicked */
  @Output() cancelClicked = new EventEmitter<void>();
  
  protected onSave(): void {
    if (!this.loading && !this.disabled) {
      this.saveClicked.emit();
    }
  }
  
  protected onCancel(): void {
    if (!this.loading) {
      this.cancelClicked.emit();
    }
  }
}
