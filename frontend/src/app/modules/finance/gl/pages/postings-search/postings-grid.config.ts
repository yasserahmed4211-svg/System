import { TranslateService } from '@ngx-translate/core';
import { ColDef, GridOptions } from 'ag-grid-community';
import { NgZone } from '@angular/core';

import {
  ERP_DEFAULT_COL_DEF,
  createErpGridOptions
} from 'src/app/shared/ag-grid';
import { SpecFieldOption, SpecOperatorOption } from 'src/app/shared/models';
import { formatAmount, formatDateValue, createStatusBadgeCellRenderer } from 'src/app/shared/utils';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

import { AccPostingMstDto, POSTING_STATUS } from '../../models/posting.model';
import { PostingActionsCellComponent } from '../../components/posting-actions-cell/posting-actions-cell.component';

export { ERP_DEFAULT_COL_DEF };

/** CSS class map for posting status badges */
const POSTING_STATUS_BADGE_MAP: Record<string, string> = {
  [POSTING_STATUS.DRAFT]:           'bg-secondary',
  [POSTING_STATUS.READY_FOR_GL]:    'bg-info text-dark',
  [POSTING_STATUS.JOURNAL_CREATED]: 'bg-primary',
  [POSTING_STATUS.READY_FOR_POST]:  'bg-warning text-dark',
  [POSTING_STATUS.POSTED]:          'bg-success',
  [POSTING_STATUS.ERROR]:           'bg-danger',
  [POSTING_STATUS.REVERSED]:        'bg-warning text-dark',
  [POSTING_STATUS.CANCELLED]:       'bg-danger',
};

/**
 * Creates filter field options for the Posting search screen.
 */
export function createPostingFilterOptions(translate: TranslateService): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const fields: SpecFieldOption[] = [
    { value: 'status', label: translate.instant('GL.POSTING_STATUS') },
    { value: 'sourceModule', label: translate.instant('GL.SOURCE_MODULE') },
    { value: 'sourceDocType', label: translate.instant('GL.SOURCE_DOC_TYPE') },
    { value: 'sourceDocNo', label: translate.instant('GL.SOURCE_DOC_NO') }
  ];

  const operators: SpecOperatorOption[] = [
    { value: 'eq', label: translate.instant('COMMON.EQUALS') },
    { value: 'like', label: translate.instant('COMMON.CONTAINS') }
  ];

  return { fields, operators };
}

/**
 * Creates column definitions for Postings AG Grid.
 */
export function createPostingColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onView: (posting: AccPostingMstDto) => void;
    onGenerateJournal: (posting: AccPostingMstDto) => void;
    onViewJournal: (posting: AccPostingMstDto) => void;
  },
  options?: { statusOptions?: LookupSelectOption[] }
): ColDef[] {
  const lang = translate.currentLang || 'en';
  const statusOptions = options?.statusOptions ?? [];

  return [
    {
      field: 'postingId',
      headerName: translate.instant('GL.POSTING_ID'),
      sortable: true,
      maxWidth: 120,
      valueFormatter: (params) => params.value != null ? String(params.value) : '—'
    },
    {
      field: 'docDate',
      headerName: translate.instant('GL.DOC_DATE'),
      sortable: true,
      flex: 1,
      minWidth: 130,
      valueFormatter: (params) => formatDateValue(params.value, lang)
    },
    {
      field: 'sourceModule',
      headerName: translate.instant('GL.SOURCE_MODULE'),
      sortable: true,
      flex: 1,
      minWidth: 130,
      tooltipField: 'sourceModule',
      valueFormatter: (params) => params.value ?? '—'
    },
    {
      field: 'sourceDocType',
      headerName: translate.instant('GL.SOURCE_DOC_TYPE'),
      sortable: true,
      flex: 1,
      minWidth: 130,
      tooltipField: 'sourceDocType',
      valueFormatter: (params) => params.value ?? '—'
    },
    {
      field: 'sourceDocNo',
      headerName: translate.instant('GL.SOURCE_DOC_NO'),
      sortable: true,
      flex: 1,
      minWidth: 140,
      tooltipField: 'sourceDocNo',
      valueFormatter: (params) => params.value ?? '—'
    },
    {
      field: 'status',
      headerName: translate.instant('GL.POSTING_STATUS'),
      sortable: true,
      flex: 1,
      minWidth: 150,
      cellRenderer: createStatusBadgeCellRenderer(POSTING_STATUS_BADGE_MAP, statusOptions)
    },
    {
      field: 'totalAmount',
      headerName: translate.instant('GL.TOTAL_AMOUNT'),
      sortable: true,
      maxWidth: 150,
      type: 'numericColumn',
      valueFormatter: (params) => formatAmount(params.value)
    },
    {
      field: 'detailCount',
      headerName: translate.instant('GL.DETAIL_COUNT'),
      sortable: false,
      maxWidth: 100,
      valueFormatter: (params) => params.value != null ? String(params.value) : '—'
    },
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      cellRenderer: PostingActionsCellComponent,
      cellRendererParams: {
        onView: (data: AccPostingMstDto) => zone.run(() => callbacks.onView(data)),
        onGenerateJournal: (data: AccPostingMstDto) => zone.run(() => callbacks.onGenerateJournal(data)),
        onViewJournal: (data: AccPostingMstDto) => zone.run(() => callbacks.onViewJournal(data))
      },
      sortable: false,
      filter: false,
      flex: 1.5,
      minWidth: 160
    }
  ];
}

/**
 * Creates grid options for the Posting AG Grid.
 */
export function createPostingGridOptions(translate: TranslateService): {
  gridOptions: GridOptions;
  localeText: Record<string, string>;
} {
  const isRtl = translate.currentLang === 'ar';
  const gridOptions = createErpGridOptions({
    enableRtl: isRtl,
    pageSize: 20
  });

  // Restore autoHeight so the grid expands to show all rows
  // (the postings template has no fixed height)
  gridOptions.domLayout = 'autoHeight';

  const localeText: Record<string, string> = {
    noRowsToShow: translate.instant('COMMON.NO_DATA'),
    page: translate.instant('COMMON.PAGE'),
    of: translate.instant('COMMON.OF'),
    to: translate.instant('COMMON.TO')
  };

  return { gridOptions, localeText };
}
