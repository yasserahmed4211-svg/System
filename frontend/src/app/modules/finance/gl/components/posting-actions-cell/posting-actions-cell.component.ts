import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { AccPostingMstDto, POSTING_STATUS } from '../../models/posting.model';

export type PostingActionsCellRendererParams = ICellRendererParams<AccPostingMstDto> & {
  onView: (posting: AccPostingMstDto) => void;
  onGenerateJournal: (posting: AccPostingMstDto) => void;
  onViewJournal: (posting: AccPostingMstDto) => void;
};

/**
 * AG Grid cell renderer for GL Posting row actions.
 *
 * Buttons shown depend on posting status:
 * - READY_FOR_GL       → View, Generate Journal
 * - JOURNAL_CREATED    → View, View Journal
 * - READY_FOR_POST     → View, View Journal
 * - POSTED             → View, View Journal
 * - CANCELLED/REVERSED → View only
 */
@Component({
  selector: 'app-posting-actions-cell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  template: `
    @if (posting; as p) {
      <div class="d-flex gap-1">
        <!-- View (always) -->
        <button
          type="button"
          class="btn btn-sm btn-outline-info"
          erpPermission="PERM_GL_POSTING_VIEW"
          [title]="'COMMON.VIEW' | translate"
          (click)="onViewClick($event)"
        >
          <i class="ti ti-eye" aria-hidden="true"></i>
        </button>

        <!-- Generate Journal (READY_FOR_GL only) -->
        @if (isReadyForGl) {
          <button
            type="button"
            class="btn btn-sm btn-outline-success"
            erpPermission="PERM_GL_POSTING_CREATE"
            [title]="'GL.GENERATE_JOURNAL' | translate"
            (click)="onGenerateJournalClick($event)"
          >
            <i class="ti ti-file-plus" aria-hidden="true"></i>
          </button>
        }

        <!-- View Journal (JOURNAL_CREATED / READY_FOR_POST / POSTED) -->
        @if (hasLinkedJournal && p.finJournalIdFk) {
          <button
            type="button"
            class="btn btn-sm btn-outline-primary"
            erpPermission="PERM_GL_JOURNAL_VIEW"
            [title]="'GL.VIEW_JOURNAL' | translate"
            (click)="onViewJournalClick($event)"
          >
            <i class="ti ti-file-text" aria-hidden="true"></i>
          </button>
        }
      </div>
    }
  `
})
export class PostingActionsCellComponent implements ICellRendererAngularComp {
  private params!: PostingActionsCellRendererParams;
  posting: AccPostingMstDto | null = null;

  get isReadyForGl(): boolean {
    return this.posting?.status === POSTING_STATUS.READY_FOR_GL;
  }

  get hasLinkedJournal(): boolean {
    const s = this.posting?.status;
    return s === POSTING_STATUS.JOURNAL_CREATED
        || s === POSTING_STATUS.READY_FOR_POST
        || s === POSTING_STATUS.POSTED;
  }

  agInit(params: PostingActionsCellRendererParams): void {
    this.params = params;
    this.posting = params.data ?? null;
  }

  refresh(params: PostingActionsCellRendererParams): boolean {
    this.params = params;
    this.posting = params.data ?? null;
    return true;
  }

  onViewClick(event: Event): void {
    event.stopPropagation();
    if (this.posting) this.params.onView(this.posting);
  }

  onGenerateJournalClick(event: Event): void {
    event.stopPropagation();
    if (this.posting) this.params.onGenerateJournal(this.posting);
  }

  onViewJournalClick(event: Event): void {
    event.stopPropagation();
    if (this.posting) this.params.onViewJournal(this.posting);
  }
}
