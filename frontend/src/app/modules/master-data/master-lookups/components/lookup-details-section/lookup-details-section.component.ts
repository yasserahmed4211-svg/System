import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import { LookupDetailDto, SearchSort } from '../../models/master-lookup.model';

/**
 * Dumb/Presentational component for displaying and interacting with lookup details.
 * Receives data via @Input and emits events via @Output.
 * Does NOT inject services or fetch data.
 */
@Component({
  selector: 'app-lookup-details-section',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ErpEmptyStateComponent,
    ErpPermissionDirective
  ],
  templateUrl: './lookup-details-section.component.html',
  styleUrl: './lookup-details-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LookupDetailsSectionComponent {
  @Input() details: LookupDetailDto[] = [];
  @Input() loading = false;
  @Input() currentSort: SearchSort | null = null;

  @Output() addDetail = new EventEmitter<void>();
  @Output() editDetail = new EventEmitter<LookupDetailDto>();
  @Output() toggleActive = new EventEmitter<LookupDetailDto>();
  @Output() deleteDetail = new EventEmitter<LookupDetailDto>();
  @Output() sortChange = new EventEmitter<string>();

  onSort(field: string): void {
    this.sortChange.emit(field);
  }

  isSortActive(field: string): boolean {
    return this.currentSort?.field === field;
  }

  getSortDirection(field: string): 'ASC' | 'DESC' | null {
    return (this.currentSort?.field === field) ? this.currentSort.direction : null;
  }
}
