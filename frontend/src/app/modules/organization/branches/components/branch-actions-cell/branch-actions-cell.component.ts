import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { BranchListItemDto } from '../../models/branch.model';

export type BranchActionsCellParams = ICellRendererParams<BranchListItemDto> & {
  onEdit: (branch: BranchListItemDto) => void;
  onDeactivate: (branch: BranchListItemDto) => void;
};

@Component({
  selector: 'app-branch-actions-cell',
  standalone: true,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (branch; as b) {
      <div class="d-flex align-items-center gap-1">
        <button
          type="button"
          class="btn btn-sm btn-outline-primary"
          erpPermission="PERM_BRANCH_UPDATE"
          [title]="'COMMON.EDIT' | translate"
          (click)="onEditClick($event, b)"
        >
          <i class="ti ti-edit"></i>
        </button>

        @if (b.statusId === 'ACTIVE') {
          <button
            type="button"
            class="btn btn-sm btn-outline-warning"
            erpPermission="PERM_BRANCH_UPDATE"
            [title]="'COMMON.DEACTIVATE' | translate"
            (click)="onDeactivateClick($event, b)"
          >
            <i class="ti ti-ban"></i>
          </button>
        }
      </div>
    }
  `
})
export class BranchActionsCellComponent implements ICellRendererAngularComp {
  private params!: BranchActionsCellParams;
  branch: BranchListItemDto | null = null;

  agInit(params: BranchActionsCellParams): void {
    this.params = params;
    this.branch = params.data ?? null;
  }

  refresh(params: BranchActionsCellParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(event: MouseEvent, branch: BranchListItemDto): void {
    event.stopPropagation();
    this.params.onEdit(branch);
  }

  onDeactivateClick(event: MouseEvent, branch: BranchListItemDto): void {
    event.stopPropagation();
    this.params.onDeactivate(branch);
  }
}
