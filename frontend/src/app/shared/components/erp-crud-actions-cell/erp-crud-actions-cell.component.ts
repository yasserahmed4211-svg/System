import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from '../../directives/erp-permission.directive';

/**
 * ErpCrudActionsCellComponent
 * 
 * Generic action cell for CRUD operations (Edit/Delete) in data grids.
 * Permission-based visibility via erpPermission directive.
 * Entity-agnostic - contains no business logic.
 * 
 * @requirement FE-REQ-SHARED-001
 * @task TASK-FE-SHARED-001
 */
@Component({
  selector: 'erp-crud-actions-cell',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ErpPermissionDirective
  ],
  template: `
    <div class="erp-action-cell">
      <button
        *ngIf="showEdit"
        [erpPermission]="editPermission"
        type="button"
        class="erp-action-btn erp-action-btn--edit"
        [attr.aria-label]="'COMMON.EDIT' | translate"
        [title]="'COMMON.EDIT' | translate"
        [disabled]="disabled"
        (click)="onEdit($event)"
      >
        <i class="fas fa-pencil-alt" aria-hidden="true"></i>
      </button>
      
      <button
        *ngIf="showDelete"
        [erpPermission]="deletePermission"
        type="button"
        class="erp-action-btn erp-action-btn--delete"
        [attr.aria-label]="'COMMON.DELETE' | translate"
        [title]="'COMMON.DELETE' | translate"
        [disabled]="disabled"
        (click)="onDelete($event)"
      >
        <i class="fas fa-trash-alt" aria-hidden="true"></i>
      </button>
      
      <!-- Additional custom actions slot -->
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    :host {
      display: inline-block;
    }
    
    .erp-action-cell {
      display: flex;
      align-items: center;
      gap: var(--erp-spacing-xs, 0.5rem);
    }
    
    .erp-action-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      inline-size: 28px;
      block-size: 28px;
      padding: 0;
      background: transparent;
      border: none;
      border-radius: var(--erp-radius-sm, 0.25rem);
      cursor: pointer;
      color: var(--erp-color-text-muted, #6c757d);
      transition: 
        color var(--erp-transition-fast, 150ms ease-in-out),
        background-color var(--erp-transition-fast, 150ms ease-in-out);
    }
    
    .erp-action-btn:hover:not(:disabled) {
      background-color: var(--erp-color-bg-subtle, #f8f9fa);
    }
    
    .erp-action-btn--edit:hover:not(:disabled) {
      color: var(--erp-color-primary, #0d6efd);
    }
    
    .erp-action-btn--delete:hover:not(:disabled) {
      color: var(--erp-color-danger, #dc3545);
    }
    
    .erp-action-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    
    .erp-action-btn:focus-visible {
      outline: 2px solid var(--erp-color-primary, #0d6efd);
      outline-offset: 2px;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpCrudActionsCellComponent {
  /** Whether to show the edit button */
  @Input() showEdit = true;
  
  /** Whether to show the delete button */
  @Input() showDelete = true;
  
  /** Permission required for the edit action */
  @Input() editPermission = '';
  
  /** Permission required for the delete action */
  @Input() deletePermission = '';
  
  /** Whether all actions are disabled */
  @Input() disabled = false;
  
  /** Emitted when the edit button is clicked */
  @Output() editClicked = new EventEmitter<void>();
  
  /** Emitted when the delete button is clicked */
  @Output() deleteClicked = new EventEmitter<void>();
  
  protected onEdit(event: Event): void {
    event.stopPropagation();
    if (!this.disabled) {
      this.editClicked.emit();
    }
  }
  
  protected onDelete(event: Event): void {
    event.stopPropagation();
    if (!this.disabled) {
      this.deleteClicked.emit();
    }
  }
}
