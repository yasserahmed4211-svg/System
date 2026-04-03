import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { SpecFieldOption, SpecFilter, SpecOperator, SpecOperatorOption, SpecValueOption } from '../../models';

interface SpecFilterRow {
  id: number;
  field: string;
  operator: SpecOperator;
  value: string | number | boolean | null;
}

@Component({
  selector: 'erp-specification-filter',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './specification-filter.component.html',
  styleUrl: './specification-filter.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SpecificationFilterComponent {
  @Input({ required: true }) availableFields: SpecFieldOption[] = [];
  @Input({ required: true }) availableOperators: SpecOperatorOption[] = [];

  @Output() apply = new EventEmitter<SpecFilter[]>();
  @Output() clear = new EventEmitter<void>();

  rows: SpecFilterRow[] = [this.createRow()];

  getOperatorsForField(field: string): SpecOperatorOption[] {
    const hasOptions = !!this.getFieldOptions(field)?.length;
    if (!hasOptions) return this.availableOperators;
    // For dropdown-based fields (e.g., boolean status), only EQ makes sense.
    return this.availableOperators.filter((op) => op.value === 'eq');
  }

  getFieldOptions(field: string): SpecValueOption[] | undefined {
    return this.availableFields?.find((f) => f.value === field)?.options;
  }

  onFieldChanged(row: SpecFilterRow): void {
    // Reset value when switching between free-text and select fields.
    const options = this.getFieldOptions(row.field);
    if (options && options.length > 0) {
      row.value = options[0].value;
      // Boolean-like dropdowns should default to EQ.
      if (row.operator !== 'eq') row.operator = 'eq';
      return;
    }
    row.value = '';
  }

  addRow(): void {
    this.rows = [...this.rows, this.createRow()];
  }

  removeRow(id: number): void {
    if (this.rows.length === 1) return;
    this.rows = this.rows.filter((r) => r.id !== id);
  }

  clearAll(): void {
    this.rows = [this.createRow()];
    this.clear.emit();
  }

  onApply(): void {
    const filters = this.toSpecFilters(this.rows);
    this.apply.emit(filters);
  }

  needsValue(operator: SpecOperator): boolean {
    return operator !== 'isNull' && operator !== 'isNotNull';
  }

  trackById(_: number, row: SpecFilterRow): number {
    return row.id;
  }

  private createRow(): SpecFilterRow {
    const defaultField = this.availableFields?.[0]?.value ?? '';
    const defaultOperator = this.availableOperators?.[0]?.value ?? 'eq';
    const defaultOptions = this.availableFields?.find((f) => f.value === defaultField)?.options;
    const defaultValue = defaultOptions && defaultOptions.length > 0 ? defaultOptions[0].value : '';

    return {
      id: Date.now() + Math.floor(Math.random() * 1000),
      field: defaultField,
      operator: defaultOperator,
      value: defaultValue
    };
  }

  private toSpecFilters(rows: SpecFilterRow[]): SpecFilter[] {
    return rows
      .map((r) => ({
        field: r.field?.trim(),
        operator: r.operator,
        value: r.value
      }))
      .filter((f) => !!f.field && !!f.operator)
      .filter((f) => {
        if (!this.needsValue(f.operator)) return true;
        return f.value !== undefined && f.value !== null && String(f.value).trim() !== '';
      })
      .map((f) => {
        if (!this.needsValue(f.operator)) {
          return { field: f.field, operator: f.operator };
        }

        if (f.operator === 'in' && typeof f.value === 'string') {
          const values = f.value
            .split(',')
            .map((v) => v.trim())
            .filter((v) => v.length > 0);
          return { ...f, value: values };
        }

        return { ...f, value: typeof f.value === 'string' ? f.value.trim() : f.value };
      });
  }
}
