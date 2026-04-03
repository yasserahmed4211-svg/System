import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';
import { AccRuleLineDto, RuleLineRequest } from '../../models/gl.model';

/** Unified type for a displayed line (may come from server or from local add). */
export type RuleLineRow = AccRuleLineDto | (RuleLineRequest & {
  _localIndex?: number;
  accountCode?: string;
  accountName?: string;
});

/**
 * Dumb/presentational component for the rule-lines card list.
 * Supports dual-column rendering with debit (green) / credit (red) visual variants.
 * Receives data via @Input, emits events via @Output.
 * Follows the master-lookups LookupDetailsSectionComponent canonical pattern.
 *
 * @requirement FE-REQ-GL-001 §4.4
 */
@Component({
  selector: 'app-rule-lines-section',
  standalone: true,
  imports: [CommonModule, TranslateModule, ErpEmptyStateComponent],
  templateUrl: './rule-lines-section.component.html',
  styleUrl: './rule-lines-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RuleLinesSectionComponent {
  @Input() lines: RuleLineRow[] = [];
  @Input() loading = false;

  /** Visual variant: 'debit' renders green accents, 'credit' renders red accents. */
  @Input() variant: 'debit' | 'credit' = 'debit';

  /** Translation key for the add button label. */
  @Input() addButtonKey = 'GL.ADD_LINE';

  /** Lookup option arrays for resolving codes to localized labels. */
  @Input() amountSourceTypeOptions: LookupSelectOption[] = [];
  @Input() entityTypeOptions: LookupSelectOption[] = [];
  @Input() paymentTypeOptions: LookupSelectOption[] = [];

  @Output() addLine = new EventEmitter<void>();
  @Output() editLine = new EventEmitter<{ line: RuleLineRow; index: number }>();
  @Output() removeLine = new EventEmitter<number>();

  /** Type-guard helper for the template to show accountCode/accountName when available. */
  asDto(line: RuleLineRow): AccRuleLineDto | null {
    return 'accountCode' in line ? (line as AccRuleLineDto) : null;
  }

  /** Resolve a raw code to its localized label from a lookup options array. */
  resolveLabel(options: LookupSelectOption[], code: string | null | undefined): string {
    if (!code) return '';
    const match = options.find(o => o.value === code);
    return match ? match.label : code;
  }
}
