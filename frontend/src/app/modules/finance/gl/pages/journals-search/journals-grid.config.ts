import { TranslateService } from '@ngx-translate/core';
import { ColDef, GridOptions, ValueFormatterParams } from 'ag-grid-community';
import { NgZone } from '@angular/core';

import {
  ERP_DEFAULT_COL_DEF,
  createErpGridOptions,
  createActiveColumnDef,
  ActiveColumnLabels
} from 'src/app/shared/ag-grid';
import { SpecFieldOption, SpecOperatorOption } from 'src/app/shared/models';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';
import {
  formatDateValue,
  formatAmount,
  createStatusBadgeCellRenderer
} from 'src/app/shared/utils';

import { GlJournalHdrDto } from '../../models/journal.model';
import { JournalActionsCellComponent } from '../../components/journal-actions-cell/journal-actions-cell.component';

// ── Journal-specific status badge class map ───────────────────────────────────

const JOURNAL_STATUS_BADGE_MAP: Record<string, string> = {
  DRAFT:     'bg-secondary',
  APPROVED:  'bg-info',
  POSTED:    'bg-success',
  REVERSED:  'bg-warning text-dark',
  CANCELLED: 'bg-danger'
};

/**
 * Creates filter field options for the Journal search screen.
 */
export function createJournalFilterOptions(
  translate: TranslateService,
  journalTypeOptions: LookupSelectOption[] = [],
  statusOptions: LookupSelectOption[] = [],
  sourceModuleOptions: LookupSelectOption[] = []
): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const fields: SpecFieldOption[] = [
    {
      value: 'journalNo',
      label: translate.instant('GL.JOURNAL_NO')
    },
    {
      value: 'journalTypeIdFk',
      label: translate.instant('GL.JOURNAL_TYPE'),
      options: journalTypeOptions.map(o => ({ value: o.value, label: o.label }))
    },
    {
      value: 'statusIdFk',
      label: translate.instant('GL.JOURNAL_STATUS'),
      options: statusOptions.map(o => ({ value: o.value, label: o.label }))
    },
    {
      value: 'sourceModuleIdFk',
      label: translate.instant('GL.SOURCE_MODULE'),
      options: sourceModuleOptions.map(o => ({ value: o.value, label: o.label }))
    },
    {
      value: 'activeFl',
      label: translate.instant('COMMON.STATUS'),
      options: [
        { value: true, label: translate.instant('COMMON.ACTIVE') },
        { value: false, label: translate.instant('COMMON.INACTIVE') }
      ]
    }
  ];

  const operators: SpecOperatorOption[] = [
    { value: 'eq', label: translate.instant('COMMON.EQUALS') },
    { value: 'like', label: translate.instant('COMMON.CONTAINS') }
  ];

  return { fields, operators };
}

/**
 * Creates column definitions for Journals AG Grid.
 */
export function createJournalColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onView: (journal: GlJournalHdrDto) => void;
    onEdit: (journal: GlJournalHdrDto) => void;
    onDeactivate: (journal: GlJournalHdrDto) => void;
    onApprove: (journal: GlJournalHdrDto) => void;
    onPost: (journal: GlJournalHdrDto) => void;
    onReverse: (journal: GlJournalHdrDto) => void;
    onCancel: (journal: GlJournalHdrDto) => void;
  },
  lookupOptions: {
    journalTypeOptions: LookupSelectOption[];
    statusOptions: LookupSelectOption[];
  } = { journalTypeOptions: [], statusOptions: [] }
): ColDef[] {
  const lang = translate.currentLang || 'ar';

  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE'),
    inactive: translate.instant('COMMON.INACTIVE'),
    all: translate.instant('COMMON.ALL')
  };

  return [
    {
      field: 'journalNo',
      headerName: translate.instant('GL.JOURNAL_NO'),
      sortable: true,
      maxWidth: 160
    },
    {
      field: 'journalDate',
      headerName: translate.instant('GL.JOURNAL_DATE'),
      sortable: true,
      flex: 1,
      minWidth: 130,
      valueFormatter: (params: ValueFormatterParams) => formatDateValue(params.value, lang)
    },
    {
      field: 'journalTypeIdFk',
      headerName: translate.instant('GL.JOURNAL_TYPE'),
      sortable: true,
      flex: 1,
      minWidth: 130,
      valueFormatter: (params: ValueFormatterParams) => {
        if (!params.value) return '—';
        return lookupOptions.journalTypeOptions.find(o => o.value === params.value)?.label ?? params.value;
      }
    },
    {
      field: 'statusIdFk',
      headerName: translate.instant('GL.JOURNAL_STATUS'),
      sortable: true,
      flex: 1,
      minWidth: 120,
      cellRenderer: createStatusBadgeCellRenderer(JOURNAL_STATUS_BADGE_MAP, lookupOptions.statusOptions)
    },
    {
      field: 'description',
      headerName: translate.instant('COMMON.DESCRIPTION'),
      sortable: true,
      flex: 2,
      minWidth: 180,
      tooltipField: 'description'
    },
    {
      field: 'totalDebit',
      headerName: translate.instant('GL.TOTAL_DEBIT'),
      sortable: true,
      maxWidth: 140,
      type: 'numericColumn',
      valueFormatter: (params: ValueFormatterParams) => formatAmount(params.value)
    },
    {
      field: 'totalCredit',
      headerName: translate.instant('GL.TOTAL_CREDIT'),
      sortable: true,
      maxWidth: 140,
      type: 'numericColumn',
      valueFormatter: (params: ValueFormatterParams) => formatAmount(params.value)
    },
    createActiveColumnDef(activeLabels, {
      headerName: translate.instant('COMMON.STATUS'),
      maxWidth: 110,
      floatingFilter: false
    }),
    {
      field: 'lineCount',
      headerName: translate.instant('GL.LINE_COUNT'),
      sortable: true,
      maxWidth: 100
    },
    {
      field: 'createdAt',
      headerName: translate.instant('COMMON.CREATED_AT'),
      sortable: true,
      flex: 1,
      minWidth: 160,
      valueFormatter: (params: ValueFormatterParams) => formatDateValue(params.value, lang)
    },
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 250,
      pinned: 'right',
      cellRenderer: JournalActionsCellComponent,
      cellRendererParams: {
        onView: (j: GlJournalHdrDto) => zone.run(() => callbacks.onView(j)),
        onEdit: (j: GlJournalHdrDto) => zone.run(() => callbacks.onEdit(j)),
        onDeactivate: (j: GlJournalHdrDto) => zone.run(() => callbacks.onDeactivate(j)),
        onApprove: (j: GlJournalHdrDto) => zone.run(() => callbacks.onApprove(j)),
        onPost: (j: GlJournalHdrDto) => zone.run(() => callbacks.onPost(j)),
        onReverse: (j: GlJournalHdrDto) => zone.run(() => callbacks.onReverse(j)),
        onCancel: (j: GlJournalHdrDto) => zone.run(() => callbacks.onCancel(j))
      }
    }
  ];
}

/**
 * Creates grid options for Journals AG Grid.
 */
export function createJournalGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const isRtl = translate.currentLang === 'ar';
  const gridOptions = createErpGridOptions({
    enableRtl: isRtl,
    pageSize: 20
  });

  const localeText: Record<string, string> = {
    empty: translate.instant('COMMON.SELECT'),
    true: translate.instant('COMMON.ACTIVE'),
    false: translate.instant('COMMON.INACTIVE')
  };

  return { gridOptions, localeText };
}

/** Re-export default column definition */
export { ERP_DEFAULT_COL_DEF };
