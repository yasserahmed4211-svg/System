import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { PageDto } from '../../models/page.model';

export type PageActionsCellRendererParams = ICellRendererParams<PageDto> & {
  onEdit: (page: PageDto) => void;
  onDeactivate: (page: PageDto) => void;
};

@Component({
  selector: 'app-page-actions-cell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective],
  template: `
    @if (page; as p) {
    <div class="d-flex align-items-center gap-1">
      <button
        type="button"
        class="btn btn-sm btn-outline-primary"
        erpPermission="PERM_PAGE_UPDATE"
        [title]="'COMMON.EDIT' | translate"
        (click)="onEditClick($event, p)"
      >
        <i class="ti ti-edit"></i>
      </button>

      <button
        type="button"
        [class]="p.active ? 'btn btn-sm btn-outline-warning' : 'btn btn-sm btn-outline-success'"
        erpPermission="PERM_PAGE_DELETE"
        [title]="p.active ? ('PAGES.DEACTIVATE' | translate) : ('PAGES.ACTIVATE' | translate)"
        (click)="onDeactivateClick($event, p)"
      >
        <i [class]="p.active ? 'ti ti-toggle-right' : 'ti ti-toggle-left'"></i>
      </button>
    </div>
    }
  `
})
export class PageActionsCellComponent implements ICellRendererAngularComp {
  private params!: PageActionsCellRendererParams;
  page: PageDto | null = null;

  agInit(params: PageActionsCellRendererParams): void {
    this.params = params;
    this.page = params.data ?? null;
  }

  refresh(params: PageActionsCellRendererParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(event: MouseEvent, page: PageDto): void {
    event.stopPropagation();
    this.params.onEdit(page);
  }

  onDeactivateClick(event: MouseEvent, page: PageDto): void {
    event.stopPropagation();
    this.params.onDeactivate(page);
  }
}
