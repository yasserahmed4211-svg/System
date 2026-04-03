import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from '../../directives/erp-permission.directive';

/**
 * ErpPageHeaderComponent
 * 
 * Generic page header with title and action buttons.
 * Entity-agnostic - contains no business logic.
 * 
 * @requirement FE-REQ-SHARED-001
 * @task TASK-FE-SHARED-001
 */
@Component({
  selector: 'erp-page-header',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ErpPermissionDirective
  ],
  template: `
    <div class="erp-page-header">
      <h1 class="erp-page-title">{{ titleKey | translate }}</h1>
      
      <div class="erp-page-actions">
        <button
          *ngIf="showRefresh"
          type="button"
          class="btn btn-outline-secondary btn-sm"
          [attr.aria-label]="'COMMON.REFRESH' | translate"
          (click)="onRefresh()"
        >
          <i class="fas fa-sync-alt" aria-hidden="true"></i>
          <span class="d-none d-sm-inline ms-1">{{ 'COMMON.REFRESH' | translate }}</span>
        </button>
        
        <button
          *ngIf="showAdd"
          [erpPermission]="addPermission"
          type="button"
          class="btn btn-primary btn-sm"
          [attr.aria-label]="'COMMON.ADD' | translate"
          (click)="onAdd()"
        >
          <i class="fas fa-plus" aria-hidden="true"></i>
          <span class="d-none d-sm-inline ms-1">{{ 'COMMON.ADD' | translate }}</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
    
    .erp-page-header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--erp-page-header-gap, 1rem);
      padding-block: var(--erp-page-header-padding-block, 1rem);
    }
    
    .erp-page-title {
      margin: 0;
      font-size: var(--erp-font-size-heading, 1.75rem);
      font-weight: var(--erp-font-weight-semibold, 600);
      line-height: var(--erp-line-height-tight, 1.25);
      color: var(--erp-color-text, inherit);
    }
    
    .erp-page-actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--erp-spacing-sm, 0.75rem);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpPageHeaderComponent {
  /** Translation key for the page title */
  @Input() titleKey = '';
  
  /** Whether to show the add button */
  @Input() showAdd = true;
  
  /** Whether to show the refresh button */
  @Input() showRefresh = true;
  
  /** Permission required to show the add button */
  @Input() addPermission = '';
  
  /** Emitted when the add button is clicked */
  @Output() addClicked = new EventEmitter<void>();
  
  /** Emitted when the refresh button is clicked */
  @Output() refreshClicked = new EventEmitter<void>();
  
  protected onAdd(): void {
    this.addClicked.emit();
  }
  
  protected onRefresh(): void {
    this.refreshClicked.emit();
  }
}
