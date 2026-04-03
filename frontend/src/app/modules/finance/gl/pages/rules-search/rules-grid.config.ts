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
import { formatDateTimeValue } from 'src/app/shared/utils';

import { AccRuleHdrDto } from '../../models/gl.model';
import { RuleActionsCellComponent } from '../../components/rule-actions-cell/rule-actions-cell.component';

/**
 * Creates filter field options for the Rules search screen.
 */
export function createRuleFilterOptions(
  translate: TranslateService,
  sourceModuleOptions: LookupSelectOption[] = [],
  sourceDocTypeOptions: LookupSelectOption[] = []
): {
  fields: SpecFieldOption[];
  operators: SpecOperatorOption[];
} {
  const fields: SpecFieldOption[] = [
    {
      value: 'sourceModule',
      label: translate.instant('GL.SOURCE_MODULE'),
      options: sourceModuleOptions.map(m => ({ value: m.value, label: m.label }))
    },
    {
      value: 'sourceDocType',
      label: translate.instant('GL.DOC_TYPE'),
      options: sourceDocTypeOptions.map(d => ({ value: d.value, label: d.label }))
    },
    {
      value: 'isActive',
      label: translate.instant('COMMON.STATUS'),
      options: [
        { value: true, label: translate.instant('COMMON.ACTIVE') },
        { value: false, label: translate.instant('COMMON.INACTIVE') }
      ]
    }
  ];

  const operators: SpecOperatorOption[] = [
    { value: 'eq', label: translate.instant('COMMON.EQUALS') }
  ];

  return { fields, operators };
}

/**
 * Creates column definitions for Rules AG Grid.
 */
export function createRuleColumnDefs(
  translate: TranslateService,
  zone: NgZone,
  callbacks: {
    onEdit: (rule: AccRuleHdrDto) => void;
    onDeactivate: (rule: AccRuleHdrDto) => void;
  },
  lookupMaps?: {
    sourceModuleMap?: Map<string, string>;
    sourceDocTypeMap?: Map<string, string>;
  }
): ColDef[] {
  const activeLabels: ActiveColumnLabels = {
    active: translate.instant('COMMON.ACTIVE'),
    inactive: translate.instant('COMMON.INACTIVE'),
    all: translate.instant('COMMON.ALL')
  };

  const resolveModule = (params: { value: string }) =>
    lookupMaps?.sourceModuleMap?.get(params.value) ?? params.value ?? '';
  const resolveDocType = (params: { value: string }) =>
    lookupMaps?.sourceDocTypeMap?.get(params.value) ?? params.value ?? '';

  const lang = translate.currentLang || 'en';

  return [
    { field: 'ruleId', headerName: translate.instant('GL.RULE_ID'), sortable: true, maxWidth: 120 },
    { field: 'companyName', headerName: translate.instant('GL.COMPANY'), sortable: true, flex: 1, minWidth: 150 },
    { field: 'sourceModule', headerName: translate.instant('GL.SOURCE_MODULE'), sortable: true, flex: 1, minWidth: 150, valueFormatter: resolveModule },
    { field: 'sourceDocType', headerName: translate.instant('GL.DOC_TYPE'), sortable: true, flex: 1, minWidth: 150, valueFormatter: resolveDocType },
    createActiveColumnDef(activeLabels, {
      field: 'isActive',
      headerName: translate.instant('COMMON.STATUS'),
      maxWidth: 120,
      floatingFilter: false
    }),
    { field: 'lineCount', headerName: translate.instant('GL.LINE_COUNT'), sortable: true, maxWidth: 120 },
    { field: 'createdAt', headerName: translate.instant('COMMON.CREATED_AT'), sortable: true, flex: 1, minWidth: 180, valueFormatter: (params: ValueFormatterParams) => formatDateTimeValue(params.value, lang) },
    {
      headerName: translate.instant('COMMON.ACTIONS'),
      maxWidth: 150,
      pinned: 'right',
      cellRenderer: RuleActionsCellComponent,
      cellRendererParams: {
        onEdit: (rule: AccRuleHdrDto) => zone.run(() => callbacks.onEdit(rule)),
        onDeactivate: (rule: AccRuleHdrDto) => zone.run(() => callbacks.onDeactivate(rule))
      }
    }
  ];
}

/**
 * Creates grid options for Rules AG Grid.
 */
export function createRuleGridOptions(translate: TranslateService): {
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
