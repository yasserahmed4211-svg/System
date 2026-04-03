// GL Posting DTOs and Models
// Aligned 1:1 with backend AccPostingMst / AccPostingDtl

// ── Posting Master ─────────────────────────────────────────

export interface AccPostingMstDto {
  postingId: number;
  branchIdFk: number | null;
  companyIdFk: number;
  currencyCode: string | null;
  docDate: string;
  errorMessage: string | null;
  finJournalIdFk: number | null;
  reversalPostingIdFk: number | null;
  sourceDocId: number;
  sourceDocNo: string | null;
  sourceDocType: string;
  sourceModule: string;
  status: string;
  totalAmount: number;
  detailCount: number;
  details?: AccPostingDtlDto[];
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string | null;
  updatedBy?: string | null;
}

// ── Posting Detail ─────────────────────────────────────────

export interface AccPostingDtlDto {
  postingDtlId: number;
  lineNo: number;
  amount: number;
  businessSide: string;
  sign: number;
  description: string | null;
  customerIdFk: number | null;
  supplierIdFk: number | null;
  costCenterIdFk: number | null;
  contractIdFk: number | null;
  itemIdFk: number | null;
}

// ── Journal Generation Response ────────────────────────────

export interface PostingGenerateJournalResponse {
  ruleId: number;
  companyIdFk: number;
  sourceModule: string;
  sourceDocType: string;
  journal: {
    id: number;
    journalNo: string;
    journalDate: string;
    journalTypeIdFk: string;
    statusIdFk: string;
    description: string | null;
  };
}

// ── Journal Preview Response ───────────────────────────────

export interface JournalPreviewLineDto {
  lineNo: number;
  accountIdFk: number;
  accountCode: string | null;
  accountName: string | null;
  debitAmount: number | null;
  creditAmount: number | null;
  customerIdFk: number | null;
  supplierIdFk: number | null;
  description: string | null;
}

export interface JournalPreviewResponse {
  ruleId: number;
  companyIdFk: number;
  sourceModule: string;
  sourceDocType: string;
  isBalanced: boolean;
  totalDebit: number;
  totalCredit: number;
  lines: JournalPreviewLineDto[];
  description: string | null;
}

// ── Posting Statuses ───────────────────────────────────────

export const POSTING_STATUS = {
  DRAFT: 'DRAFT',
  READY_FOR_GL: 'READY_FOR_GL',
  JOURNAL_CREATED: 'JOURNAL_CREATED',
  READY_FOR_POST: 'READY_FOR_POST',
  POSTED: 'POSTED',
  ERROR: 'ERROR',
  REVERSED: 'REVERSED',
  CANCELLED: 'CANCELLED'
} as const;
