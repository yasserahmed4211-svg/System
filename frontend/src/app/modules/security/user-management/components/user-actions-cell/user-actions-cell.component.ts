import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { UserDto } from '../../models/user.model';

export type UserActionsCellRendererParams = ICellRendererParams<UserDto> & {
  onEdit: (user: UserDto) => void;
  onDelete: (user: UserDto) => void;
};

@Component({
  selector: 'app-user-actions-cell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  template: `
    <div class="d-flex align-items-center gap-1" *ngIf="user as u">
      <button
        type="button"
        class="btn btn-sm btn-info"
        erpPermission="PERM_USER_UPDATE"
        [title]="'COMMON.EDIT' | translate"
        (click)="onEditClick($event, u)"
      >
        <i class="ti ti-edit"></i>
      </button>

      <button
        type="button"
        class="btn btn-sm btn-danger"
        erpPermission="PERM_USER_DELETE"
        [title]="'COMMON.DELETE' | translate"
        (click)="onDeleteClick($event, u)"
      >
        <i class="ti ti-trash"></i>
      </button>
    </div>
  `
})
export class UserActionsCellComponent implements ICellRendererAngularComp {
  private params!: UserActionsCellRendererParams;
  user: UserDto | null = null;

  agInit(params: UserActionsCellRendererParams): void {
    this.params = params;
    this.user = params.data ?? null;
  }

  refresh(params: UserActionsCellRendererParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(event: MouseEvent, user: UserDto): void {
    event.stopPropagation();
    this.params.onEdit(user);
  }

  onDeleteClick(event: MouseEvent, user: UserDto): void {
    event.stopPropagation();
    this.params.onDelete(user);
  }
}
