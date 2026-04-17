import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { RegionListItemDto } from '../../models/region.model';

export type RegionActionsCellParams = ICellRendererParams<RegionListItemDto> & {
  onEdit: (region: RegionListItemDto) => void;
  onDeactivate: (region: RegionListItemDto) => void;
};

@Component({
  selector: 'app-region-actions-cell',
  standalone: true,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (region; as r) {
      <div class="d-flex align-items-center gap-1">
        <button
          type="button"
          class="btn btn-sm btn-outline-primary"
          erpPermission="PERM_REGION_UPDATE"
          [title]="'COMMON.EDIT' | translate"
          (click)="onEditClick($event, r)"
        >
          <i class="ti ti-edit"></i>
        </button>

        @if (r.statusId === 'ACTIVE') {
          <button
            type="button"
            class="btn btn-sm btn-outline-warning"
            erpPermission="PERM_REGION_UPDATE"
            [title]="'COMMON.DEACTIVATE' | translate"
            (click)="onDeactivateClick($event, r)"
          >
            <i class="ti ti-ban"></i>
          </button>
        }
      </div>
    }
  `
})
export class RegionActionsCellComponent implements ICellRendererAngularComp {
  private params!: RegionActionsCellParams;
  region: RegionListItemDto | null = null;

  agInit(params: RegionActionsCellParams): void {
    this.params = params;
    this.region = params.data ?? null;
  }

  refresh(params: RegionActionsCellParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(event: MouseEvent, region: RegionListItemDto): void {
    event.stopPropagation();
    this.params.onEdit(region);
  }

  onDeactivateClick(event: MouseEvent, region: RegionListItemDto): void {
    event.stopPropagation();
    this.params.onDeactivate(region);
  }
}
