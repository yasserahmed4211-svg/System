import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

/**
 * ErpEmptyStateComponent
 *
 * Pure UI empty-state used for tables and lists.
 */
@Component({
  selector: 'erp-empty-state',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './erp-empty-state.component.html',
  styleUrls: ['./erp-empty-state.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpEmptyStateComponent {
  @Input() titleKey = '';
  @Input() messageKey = '';
  @Input() icon?: string;
  @Input() actionLabelKey?: string;

  @Output() actionClicked = new EventEmitter<void>();
}
