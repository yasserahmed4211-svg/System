import {
  Component, Input, Output, EventEmitter, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import { DepartmentDto } from '../../models/branch.model';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

/**
 * Dumb/Presentational component for inline department management within branch entry.
 * Does NOT inject services. All actions emitted via @Output.
 */
@Component({
  selector: 'app-departments-section',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ReactiveFormsModule,
    FormsModule,
    ErpPermissionDirective
  ],
  templateUrl: './departments-section.component.html',
  styleUrl: './departments-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DepartmentsSectionComponent {
  @Input() departments: DepartmentDto[] = [];
  @Input() saving = false;
  @Input() isEditMode = false;

  /** Department type options — passed from parent (loaded via facade/translate) */
  @Input() departmentTypeOptions: LookupSelectOption[] = [];

  /** Per-row error flags — true means that row has validation errors */
  @Input() rowErrors: boolean[] = [];

  /** True when there are unsaved rows with validation errors (show summary) */
  @Input() showValidationSummary = false;

  /** Emits when user clicks "Add row" */
  @Output() addRow = new EventEmitter<void>();

  /** Emits index of row that should be removed (unsaved) */
  @Output() removeRow = new EventEmitter<number>();

  /** Emits a saved row that should be deactivated (confirm flow in parent) */
  @Output() deactivateRow = new EventEmitter<DepartmentDto>();

  /** Emits field change for a specific row — parent updates local state */
  @Output() changeField = new EventEmitter<{ index: number; field: keyof DepartmentDto; value: unknown }>();

  /** Emits when user clicks "Save All" for unsaved rows */
  @Output() saveAll = new EventEmitter<void>();

  onAddRow(): void { this.addRow.emit(); }

  onRemoveRow(index: number): void { this.removeRow.emit(index); }

  onDeactivateRow(dept: DepartmentDto): void { this.deactivateRow.emit(dept); }

  onFieldChange(index: number, field: keyof DepartmentDto, value: unknown): void {
    this.changeField.emit({ index, field, value });
  }

  onSaveAll(): void { this.saveAll.emit(); }

  /** Determine if a row is persisted (has a real PK > 0) */
  isSaved(dept: DepartmentDto): boolean {
    return !!dept.departmentPk;
  }

  /** True when there are rows not yet persisted to the backend */
  get hasUnsavedRows(): boolean {
    return this.departments.some(d => !d.departmentPk);
  }

  isRowInvalid(index: number): boolean {
    return !!this.rowErrors[index];
  }
}
