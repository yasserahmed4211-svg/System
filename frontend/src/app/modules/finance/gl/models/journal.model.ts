// GL Journal DTOs and Models
// Aligned 1:1 with backend GlJournalHdr / GlJournalLine

// ── Journal Header ─────────────────────────────────────────

export interface GlJournalHdrDto {
  id: number;
  journalNo: string;
  journalDate: string;
  journalTypeIdFk: string;
  statusIdFk: string;
  description: string | null;
  sourceModuleIdFk: string | null;
  sourceDocTypeId: string | null;
  sourceDocIdFk: number | null;
  sourcePostingIdFk: number | null;
  totalDebit: number;
  totalCredit: number;
  activeFl: boolean;
  lineCount: number;
  lines?: GlJournalLineDto[];
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string | null;
  updatedBy?: string | null;
}

// ── Journal Line ───────────────────────────────────────────

export interface GlJournalLineDto {
  id: number;
  journalIdFk: number;
  lineNo: number;
  accountIdFk: number;
  accountCode: string | null;
  accountName: string | null;
  debitAmount: number | null;
  creditAmount: number | null;
  customerIdFk: number | null;
  supplierIdFk: number | null;
  costCenterIdFk: number | null;
  description: string | null;
}

// ── Create Request ─────────────────────────────────────────

export interface CreateJournalRequest {
  journalDate: string;
  journalTypeIdFk: string;
  description?: string | null;
  sourceModuleIdFk?: string | null;
  sourceDocTypeId?: string | null;
  sourceDocIdFk?: number | null;
  sourcePostingIdFk?: number | null;
  lines: JournalLineRequest[];
}

// ── Update Request ─────────────────────────────────────────

export interface UpdateJournalRequest {
  journalDate: string;
  journalTypeIdFk: string;
  description?: string | null;
  sourceModuleIdFk?: string | null;
  sourceDocTypeId?: string | null;
  sourceDocIdFk?: number | null;
  sourcePostingIdFk?: number | null;
  lines: JournalLineRequest[];
}

// ── Line Request ───────────────────────────────────────────

export interface JournalLineRequest {
  accountIdFk: number;
  debitAmount?: number | null;
  creditAmount?: number | null;
  customerIdFk?: number | null;
  supplierIdFk?: number | null;
  costCenterIdFk?: number | null;
  description?: string | null;
}

// ── Lookup Keys ────────────────────────────────────────────

export const JOURNAL_LOOKUP_KEYS = {
  JOURNAL_TYPE: 'GL_JOURNAL_TYPE',
  JOURNAL_STATUS: 'GL_JOURNAL_STATUS',
  SOURCE_MODULE: 'SOURCE_MODULE'
} as const;

// ── Journal Statuses ───────────────────────────────────────

export const JOURNAL_STATUS = {
  DRAFT: 'DRAFT',
  APPROVED: 'APPROVED',
  POSTED: 'POSTED',
  REVERSED: 'REVERSED',
  CANCELLED: 'CANCELLED'
} as const;

export const JOURNAL_TYPE = {
  MANUAL: 'MANUAL',
  AUTOMATIC: 'AUTOMATIC',
  ADJUSTMENT: 'ADJUSTMENT',
  REVERSAL: 'REVERSAL'
} as const;

// ── Manual Journal Request DTOs ────────────────────────────

export interface ManualCreateJournalRequest {
  journalDate: string;
  description?: string | null;
  lines: ManualJournalLineRequest[];
}

export interface ManualUpdateJournalRequest {
  journalDate: string;
  description?: string | null;
  lines: ManualJournalLineRequest[];
}

export interface ManualJournalLineRequest {
  accountIdFk: number;
  debitAmount?: number | null;
  creditAmount?: number | null;
  description?: string | null;
}
