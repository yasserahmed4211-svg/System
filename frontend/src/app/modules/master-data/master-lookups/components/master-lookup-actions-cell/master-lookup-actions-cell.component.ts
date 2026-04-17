import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { MasterLookupDto } from '../../models/master-lookup.model';

export type MasterLookupActionsCellParams = ICellRendererParams<MasterLookupDto> & {
  onEdit: (lookup: MasterLookupDto) => void;
  onToggleActive: (lookup: MasterLookupDto) => void;
  onDelete: (lookup: MasterLookupDto) => void;
};

@Component({
  selector: 'app-master-lookup-actions-cell',
  standalone: true,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (lookup; as l) {
    <div class="d-flex align-items-center gap-1">
      <!-- Edit -->
      <button
        type="button"
        class="btn btn-sm btn-outline-primary"
        erpPermission="PERM_MASTER_LOOKUP_UPDATE"
        [title]="'COMMON.EDIT' | translate"
        (click)="onEditClick($event, l)"
      >
        <i class="ti ti-edit"></i>
      </button>

      <!-- Activate/Deactivate -->
      <button
        type="button"
        [class]="l.isActive ? 'btn btn-sm btn-outline-warning' : 'btn btn-sm btn-outline-success'"
        erpPermission="PERM_MASTER_LOOKUP_UPDATE"
        [title]="l.isActive ? ('MASTER_LOOKUPS.DEACTIVATE' | translate) : ('MASTER_LOOKUPS.ACTIVATE' | translate)"
        (click)="onToggleActiveClick($event, l)"
      >
        <i [class]="l.isActive ? 'ti ti-toggle-right' : 'ti ti-toggle-left'"></i>
      </button>

      <!-- Delete -->
      <button
        type="button"
        class="btn btn-sm btn-outline-danger"
        erpPermission="PERM_MASTER_LOOKUP_DELETE"
        [title]="'COMMON.DELETE' | translate"
        (click)="onDeleteClick($event, l)"
      >
        <i class="ti ti-trash"></i>
      </button>
    </div>
    }
  `
})
export class MasterLookupActionsCellComponent implements ICellRendererAngularComp {
  private params!: MasterLookupActionsCellParams;
  lookup: MasterLookupDto | null = null;

  agInit(params: MasterLookupActionsCellParams): void {
    this.params = params;
    this.lookup = params.data ?? null;
  }

  refresh(params: MasterLookupActionsCellParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(event: MouseEvent, lookup: MasterLookupDto): void {
    event.stopPropagation();
    this.params.onEdit(lookup);
  }

  onToggleActiveClick(event: MouseEvent, lookup: MasterLookupDto): void {
    event.stopPropagation();
    this.params.onToggleActive(lookup);
  }

  onDeleteClick(event: MouseEvent, lookup: MasterLookupDto): void {
    event.stopPropagation();
    this.params.onDelete(lookup);
  }
}
