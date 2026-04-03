import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { ErpCrudActionsCellComponent } from 'src/app/shared/components/erp-crud-actions-cell/erp-crud-actions-cell.component';
import { GlJournalHdrDto, JOURNAL_STATUS } from '../../models/journal.model';

export type JournalActionsCellRendererParams = ICellRendererParams<GlJournalHdrDto> & {
  onView: (journal: GlJournalHdrDto) => void;
  onEdit: (journal: GlJournalHdrDto) => void;
  onDeactivate: (journal: GlJournalHdrDto) => void;
  onApprove: (journal: GlJournalHdrDto) => void;
  onPost: (journal: GlJournalHdrDto) => void;
  onReverse: (journal: GlJournalHdrDto) => void;
  onCancel: (journal: GlJournalHdrDto) => void;
};

/**
 * AG Grid cell renderer for GL Journal row actions.
 *
 * Buttons shown depend on journal status:
 * - DRAFT     → Edit, Approve, Cancel, Deactivate
 * - APPROVED  → Post, Cancel
 * - POSTED    → Reverse
 * - REVERSED / CANCELLED → View only (no action buttons)
 */
@Component({
  selector: 'app-journal-actions-cell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective, ErpCrudActionsCellComponent],
  template: `
    @if (journal; as j) {
      <erp-crud-actions-cell
        [showEdit]="isDraft"
        [showDelete]="false"
        editPermission="PERM_GL_JOURNAL_UPDATE"
        (editClicked)="onEditClick()"
      >
        <!-- View (always available) -->
        <button
          type="button"
          class="btn btn-sm btn-outline-info"
          erpPermission="PERM_GL_JOURNAL_VIEW"
          [title]="'COMMON.VIEW' | translate"
          (click)="onViewClick($event)"
        >
          <i class="ti ti-eye" aria-hidden="true"></i>
        </button>

        <!-- Approve (DRAFT only) -->
        @if (isDraft) {
          <button
            type="button"
            class="btn btn-sm btn-outline-success"
            erpPermission="PERM_GL_JOURNAL_UPDATE"
            [title]="'GL.APPROVE' | translate"
            (click)="onApproveClick($event)"
          >
            <i class="ti ti-check" aria-hidden="true"></i>
          </button>
        }

        <!-- Post (APPROVED only) -->
        @if (isApproved) {
          <button
            type="button"
            class="btn btn-sm btn-outline-primary"
            erpPermission="PERM_GL_JOURNAL_UPDATE"
            [title]="'GL.POST' | translate"
            (click)="onPostClick($event)"
          >
            <i class="ti ti-send" aria-hidden="true"></i>
          </button>
        }

        <!-- Reverse (POSTED only) -->
        @if (isPosted) {
          <button
            type="button"
            class="btn btn-sm btn-outline-warning"
            erpPermission="PERM_GL_JOURNAL_UPDATE"
            [title]="'GL.REVERSE' | translate"
            (click)="onReverseClick($event)"
          >
            <i class="ti ti-arrow-back-up" aria-hidden="true"></i>
          </button>
        }

        <!-- Cancel (DRAFT or APPROVED) -->
        @if (isDraft || isApproved) {
          <button
            type="button"
            class="btn btn-sm btn-outline-danger"
            erpPermission="PERM_GL_JOURNAL_UPDATE"
            [title]="'GL.CANCEL_JOURNAL' | translate"
            (click)="onCancelClick($event)"
          >
            <i class="ti ti-x" aria-hidden="true"></i>
          </button>
        }

        <!-- Deactivate toggle (DRAFT only) -->
        @if (isDraft) {
          <button
            type="button"
            [class]="j.activeFl ? 'btn btn-sm btn-outline-warning' : 'btn btn-sm btn-outline-success'"
            erpPermission="PERM_GL_JOURNAL_DELETE"
            [title]="j.activeFl ? ('COMMON.DEACTIVATE' | translate) : ('COMMON.ACTIVATE' | translate)"
            (click)="onDeactivateClick($event)"
          >
            <i [class]="j.activeFl ? 'ti ti-toggle-right' : 'ti ti-toggle-left'" aria-hidden="true"></i>
          </button>
        }
      </erp-crud-actions-cell>
    }
  `
})
export class JournalActionsCellComponent implements ICellRendererAngularComp {
  private params!: JournalActionsCellRendererParams;
  journal: GlJournalHdrDto | null = null;

  get isDraft(): boolean { return this.journal?.statusIdFk === JOURNAL_STATUS.DRAFT; }
  get isApproved(): boolean { return this.journal?.statusIdFk === JOURNAL_STATUS.APPROVED; }
  get isPosted(): boolean { return this.journal?.statusIdFk === JOURNAL_STATUS.POSTED; }

  agInit(params: JournalActionsCellRendererParams): void {
    this.params = params;
    this.journal = params.data ?? null;
  }

  refresh(params: JournalActionsCellRendererParams): boolean {
    this.agInit(params);
    return true;
  }

  onViewClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.journal) this.params.onView(this.journal);
  }

  onEditClick(): void {
    if (this.journal) this.params.onEdit(this.journal);
  }

  onDeactivateClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.journal) this.params.onDeactivate(this.journal);
  }

  onApproveClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.journal) this.params.onApprove(this.journal);
  }

  onPostClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.journal) this.params.onPost(this.journal);
  }

  onReverseClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.journal) this.params.onReverse(this.journal);
  }

  onCancelClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.journal) this.params.onCancel(this.journal);
  }
}
