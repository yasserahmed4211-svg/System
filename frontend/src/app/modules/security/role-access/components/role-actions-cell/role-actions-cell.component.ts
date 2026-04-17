import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { RoleDto } from '../../models/role-access.model';

export type RoleActionsCellRendererParams = ICellRendererParams<RoleDto> & {
  onEdit: (role: RoleDto) => void;
  onToggleActive: (role: RoleDto) => void;
  onDelete: (role: RoleDto) => void;
};

@Component({
  selector: 'app-role-actions-cell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  template: `
    @if (role; as r) {
    <div class="d-flex align-items-center gap-1">
      <button
        type="button"
        class="btn btn-sm btn-outline-primary"
        erpPermission="ROLE.UPDATE"
        [title]="'COMMON.EDIT' | translate"
        (click)="onEditClick($event, r)"
      >
        <i class="ti ti-edit"></i>
      </button>

      <button
        type="button"
        [class]="r.active ? 'btn btn-sm btn-outline-warning' : 'btn btn-sm btn-outline-success'"
        erpPermission="ROLE.DELETE"
        [title]="r.active ? ('COMMON.DEACTIVATE' | translate) : ('COMMON.ACTIVATE' | translate)"
        (click)="onToggleActiveClick($event, r)"
      >
        <i [class]="r.active ? 'ti ti-toggle-right' : 'ti ti-toggle-left'"></i>
      </button>

      <button
        type="button"
        class="btn btn-sm btn-outline-danger"
        erpPermission="ROLE.DELETE"
        [title]="'COMMON.DELETE' | translate"
        (click)="onDeleteClick($event, r)"
      >
        <i class="ti ti-trash"></i>
      </button>
    </div>
    }
  `
})
export class RoleActionsCellComponent implements ICellRendererAngularComp {
  private params!: RoleActionsCellRendererParams;
  role: RoleDto | null = null;

  agInit(params: RoleActionsCellRendererParams): void {
    this.params = params;
    this.role = params.data ?? null;
  }

  refresh(params: RoleActionsCellRendererParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(event: MouseEvent, role: RoleDto): void {
    event.stopPropagation();
    this.params.onEdit(role);
  }

  onToggleActiveClick(event: MouseEvent, role: RoleDto): void {
    event.stopPropagation();
    this.params.onToggleActive(role);
  }

  onDeleteClick(event: MouseEvent, role: RoleDto): void {
    event.stopPropagation();
    this.params.onDelete(role);
  }
}
