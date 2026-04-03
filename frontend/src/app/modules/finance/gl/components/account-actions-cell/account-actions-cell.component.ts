import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { ErpCrudActionsCellComponent } from 'src/app/shared/components/erp-crud-actions-cell/erp-crud-actions-cell.component';
import { AccountChartDto } from '../../models/gl.model';

export type AccountActionsCellRendererParams = ICellRendererParams<AccountChartDto> & {
  onEdit: (account: AccountChartDto) => void;
  onDeactivate: (account: AccountChartDto) => void;
};

/**
 * AG Grid cell renderer for account row actions.
 * Delegates to shared ErpCrudActionsCellComponent for edit; adds domain-specific deactivate toggle.
 *
 * @requirement FE-REQ-GL-001 §3.6
 * @task TASK-FE-GL-001
 */
@Component({
  selector: 'app-account-actions-cell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective, ErpCrudActionsCellComponent],
  template: `
    @if (account; as a) {
      <erp-crud-actions-cell
        [showEdit]="true"
        [showDelete]="false"
        editPermission="PERM_GL_ACCOUNT_UPDATE"
        (editClicked)="onEditClick()"
      >
        <button
          type="button"
          [class]="a.isActive ? 'btn btn-sm btn-outline-warning' : 'btn btn-sm btn-outline-success'"
          erpPermission="PERM_GL_ACCOUNT_DELETE"
          [title]="a.isActive ? ('COMMON.DEACTIVATE' | translate) : ('COMMON.ACTIVATE' | translate)"
          (click)="onDeactivateClick($event)"
        >
          <i [class]="a.isActive ? 'ti ti-toggle-right' : 'ti ti-toggle-left'" aria-hidden="true"></i>
        </button>
      </erp-crud-actions-cell>
    }
  `
})
export class AccountActionsCellComponent implements ICellRendererAngularComp {
  private params!: AccountActionsCellRendererParams;
  account: AccountChartDto | null = null;

  agInit(params: AccountActionsCellRendererParams): void {
    this.params = params;
    this.account = params.data ?? null;
  }

  refresh(params: AccountActionsCellRendererParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(): void {
    if (this.account) this.params.onEdit(this.account);
  }

  onDeactivateClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.account) this.params.onDeactivate(this.account);
  }
}
