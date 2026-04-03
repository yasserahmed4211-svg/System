import {
  Component, ChangeDetectionStrategy, inject, Input
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { JournalPreviewResponse, AccPostingMstDto } from '../../models/posting.model';

/**
 * Modal component that shows a journal preview before generation.
 * Displays:
 * - Posting summary (docNo, type, date, amount)
 * - Preview journal lines (account, debit, credit)
 * - Balance validation indicator
 * - Warning about irreversibility
 * - Cancel / Confirm Generate buttons
 *
 * Opened via NgbModal.open(). Returns 'confirm' on close or dismisses on cancel.
 */
@Component({
  selector: 'app-posting-journal-preview-modal',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <div class="modal-header bg-light">
      <h5 class="modal-title">
        <i class="ti ti-file-search me-2 text-primary"></i>{{ 'GL.JOURNAL_PREVIEW' | translate }}
      </h5>
      <button type="button" class="btn-close" aria-label="Close" (click)="onCancel()"></button>
    </div>

    <div class="modal-body">
      <!-- Posting Info Summary -->
      <div class="row g-2 mb-3">
        <div class="col-md-3">
          <small class="text-muted d-block">{{ 'GL.POSTING_ID' | translate }}</small>
          <span class="fw-semibold">{{ posting.postingId }}</span>
        </div>
        <div class="col-md-3">
          <small class="text-muted d-block">{{ 'GL.SOURCE_DOC_NO' | translate }}</small>
          <span class="fw-semibold">{{ posting.sourceDocNo || '-' }}</span>
        </div>
        <div class="col-md-3">
          <small class="text-muted d-block">{{ 'GL.DOC_DATE' | translate }}</small>
          <span class="fw-semibold">{{ posting.docDate }}</span>
        </div>
        <div class="col-md-3">
          <small class="text-muted d-block">{{ 'GL.TOTAL_AMOUNT' | translate }}</small>
          <span class="fw-semibold">{{ posting.totalAmount | number:'1.2-2' }} {{ posting.currencyCode }}</span>
        </div>
      </div>

      <hr />

      <!-- Rule Info -->
      <div class="d-flex justify-content-between align-items-center mb-2">
        <small class="text-muted">
          {{ 'GL.ACCOUNTING_RULE' | translate }}: <strong>{{ preview.ruleId }}</strong>
          &nbsp;|&nbsp; {{ preview.sourceModule }} / {{ preview.sourceDocType }}
        </small>
      </div>

      <!-- Preview Journal Lines Table -->
      <div class="table-responsive">
        <table class="table table-sm table-striped mb-0">
          <thead class="table-light">
            <tr>
              <th>#</th>
              <th>{{ 'GL.ACCOUNT_CODE' | translate }}</th>
              <th>{{ 'GL.ACCOUNT_NAME' | translate }}</th>
              <th class="text-end">{{ 'GL.DEBIT' | translate }}</th>
              <th class="text-end">{{ 'GL.CREDIT' | translate }}</th>
              <th>{{ 'COMMON.DESCRIPTION' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (line of preview.lines; track line.lineNo) {
              <tr>
                <td>{{ line.lineNo }}</td>
                <td><code>{{ line.accountCode || '-' }}</code></td>
                <td>{{ line.accountName || '-' }}</td>
                <td class="text-end">
                  @if (line.debitAmount) {
                    <span class="text-success">{{ line.debitAmount | number:'1.2-2' }}</span>
                  } @else {
                    -
                  }
                </td>
                <td class="text-end">
                  @if (line.creditAmount) {
                    <span class="text-danger">{{ line.creditAmount | number:'1.2-2' }}</span>
                  } @else {
                    -
                  }
                </td>
                <td><small class="text-muted">{{ line.description || '-' }}</small></td>
              </tr>
            }
          </tbody>
          <tfoot class="table-light fw-bold">
            <tr>
              <td colspan="3" class="text-end">{{ 'GL.TOTALS' | translate }}</td>
              <td class="text-end text-success">{{ preview.totalDebit | number:'1.2-2' }}</td>
              <td class="text-end text-danger">{{ preview.totalCredit | number:'1.2-2' }}</td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- Balance Indicator -->
      <div class="mt-3">
        @if (preview.isBalanced) {
          <div class="alert alert-success py-2 mb-0 d-flex align-items-center">
            <i class="ti ti-circle-check me-2 fs-5"></i>
            <span>{{ 'GL.JOURNAL_BALANCED' | translate }}</span>
          </div>
        } @else {
          <div class="alert alert-danger py-2 mb-0 d-flex align-items-center">
            <i class="ti ti-alert-triangle me-2 fs-5"></i>
            <span>{{ 'GL.JOURNAL_NOT_BALANCED' | translate }}</span>
          </div>
        }
      </div>

      <!-- Warning -->
      <div class="alert alert-warning py-2 mt-3 mb-0 d-flex align-items-start">
        <i class="ti ti-alert-circle me-2 mt-1"></i>
        <small>{{ 'GL.JOURNAL_PREVIEW_WARNING' | translate }}</small>
      </div>
    </div>

    <div class="modal-footer">
      <button type="button" class="btn btn-secondary" (click)="onCancel()">
        {{ 'COMMON.CANCEL' | translate }}
      </button>
      <button
        type="button"
        class="btn btn-success"
        (click)="onConfirm()"
        [disabled]="!preview.isBalanced"
      >
        <i class="ti ti-file-plus me-1"></i>{{ 'GL.GENERATE_JOURNAL' | translate }}
      </button>
    </div>
  `,
  styles: [`
    .modal-header {
      border-bottom: 2px solid var(--bs-primary, #0d6efd);
    }
    .modal-body {
      max-height: 70vh;
      overflow-y: auto;
    }
    .table th, .table td {
      vertical-align: middle;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostingJournalPreviewModalComponent {
  private readonly activeModal = inject(NgbActiveModal);

  @Input() posting!: AccPostingMstDto;
  @Input() preview!: JournalPreviewResponse;

  onConfirm(): void {
    this.activeModal.close('confirm');
  }

  onCancel(): void {
    this.activeModal.dismiss('cancel');
  }
}
