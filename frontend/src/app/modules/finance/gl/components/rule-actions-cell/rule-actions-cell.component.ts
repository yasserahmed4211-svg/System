import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TranslateModule } from '@ngx-translate/core';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { ErpCrudActionsCellComponent } from 'src/app/shared/components/erp-crud-actions-cell/erp-crud-actions-cell.component';
import { AccRuleHdrDto } from '../../models/gl.model';

export type RuleActionsCellRendererParams = ICellRendererParams<AccRuleHdrDto> & {
  onEdit: (rule: AccRuleHdrDto) => void;
  onDeactivate: (rule: AccRuleHdrDto) => void;
};

/**
 * AG Grid cell renderer for accounting rule row actions.
 * Delegates to shared ErpCrudActionsCellComponent for edit; adds domain-specific deactivate toggle.
 *
 * @requirement FE-REQ-GL-001 §4.6
 * @task TASK-FE-GL-001
 */
@Component({
  selector: 'app-rule-actions-cell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TranslateModule, ErpPermissionDirective, ErpCrudActionsCellComponent],
  template: `
    @if (rule; as r) {
      <erp-crud-actions-cell
        [showEdit]="true"
        [showDelete]="false"
        editPermission="PERM_GL_RULE_UPDATE"
        (editClicked)="onEditClick()"
      >
        <button
          type="button"
          [class]="r.isActive ? 'btn btn-sm btn-outline-warning' : 'btn btn-sm btn-outline-success'"
          erpPermission="PERM_GL_RULE_DELETE"
          [title]="r.isActive ? ('COMMON.DEACTIVATE' | translate) : ('COMMON.ACTIVATE' | translate)"
          (click)="onDeactivateClick($event)"
        >
          <i [class]="r.isActive ? 'ti ti-toggle-right' : 'ti ti-toggle-left'" aria-hidden="true"></i>
        </button>
      </erp-crud-actions-cell>
    }
  `
})
export class RuleActionsCellComponent implements ICellRendererAngularComp {
  private params!: RuleActionsCellRendererParams;
  rule: AccRuleHdrDto | null = null;

  agInit(params: RuleActionsCellRendererParams): void {
    this.params = params;
    this.rule = params.data ?? null;
  }

  refresh(params: RuleActionsCellRendererParams): boolean {
    this.agInit(params);
    return true;
  }

  onEditClick(): void {
    if (this.rule) this.params.onEdit(this.rule);
  }

  onDeactivateClick(event: MouseEvent): void {
    event.stopPropagation();
    if (this.rule) this.params.onDeactivate(this.rule);
  }
}
