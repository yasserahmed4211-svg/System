import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { LegalEntityListItemDto } from '../../models/legal-entity.model';

export type LegalEntityActionsCellParams = ICellRendererParams<LegalEntityListItemDto> & {
  onEdit: (entity: LegalEntityListItemDto) => void;
  onDeactivate: (entity: LegalEntityListItemDto) => void;
};

@Component({
  selector: 'app-legal-entity-actions-cell',
  standalone: true,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (entity; as e) {
      <div class="d-flex align-items-center gap-1">
        <!-- Edit -->
        <button
          type="button"
          class="btn btn-sm btn-outline-primary"
          erpPermission="PERM_LEGAL_ENTITY_UPDATE"
          [title]="'COMMON.EDIT' | translate"
          (click)="onEditClick($event, e)"
        >
          <i class="ti ti-edit"></i>
        </button>

        <!-- Deactivate — only shown when active -->
        @if (e.activeFl === 1) {
          <button
            type="button"
            class="btn btn-sm btn-outline-warning"
            erpPermission="PERM_LEGAL_ENTITY_UPDATE"
            [title]="'COMMON.DEACTIVATE' | translate"
            (click)="onDeactivateClick($event, e)"
          >
            <i class="ti ti-ban"></i>
          </button>
        }
      </div>
    }
  `
})
export class LegalEntityActionsCellComponent implements ICellRendererAngularComp {
  private params!: LegalEntityActionsCellParams;
  entity: LegalEntityListItemDto | null = null;

  agInit(params: LegalEntityActionsCellParams): void {
    this.params = params;
    this.entity = params.data ?? null;
  }

  refresh(params: LegalEntityActionsCellParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(event: MouseEvent, entity: LegalEntityListItemDto): void {
    event.stopPropagation();
    this.params.onEdit(entity);
  }

  onDeactivateClick(event: MouseEvent, entity: LegalEntityListItemDto): void {
    event.stopPropagation();
    this.params.onDeactivate(entity);
  }
}
