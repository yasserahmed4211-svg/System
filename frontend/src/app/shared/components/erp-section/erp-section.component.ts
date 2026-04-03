import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

/**
 * ErpSectionComponent
 *
 * Pure UI wrapper for grouping fields inside a form.
 */
@Component({
  selector: 'erp-section',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './erp-section.component.html',
  styleUrls: ['./erp-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpSectionComponent {
  @Input() titleKey = '';
  @Input() descriptionKey?: string;
}
