import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

/**
 * ErpReadonlyHintComponent
 *
 * Pure UI readonly label/value pair for view/edit modes.
 */
@Component({
  selector: 'erp-readonly-hint',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './erp-readonly-hint.component.html',
  styleUrls: ['./erp-readonly-hint.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpReadonlyHintComponent {
  @Input() labelKey = '';
  @Input() value: string | number | null = null;
  @Input() placeholderKey = 'COMMON.NO_DATA';

  get hasValue(): boolean {
    return this.value !== null && this.value !== undefined && `${this.value}`.length > 0;
  }
}
