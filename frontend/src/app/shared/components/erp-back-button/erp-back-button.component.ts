import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

/**
 * ErpBackButtonComponent
 *
 * Pure UI back trigger. No routing decisions inside.
 */
@Component({
  selector: 'erp-back-button',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './erp-back-button.component.html',
  styleUrls: ['./erp-back-button.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpBackButtonComponent {
  @Output() backClicked = new EventEmitter<void>();

  onBack(): void {
    this.backClicked.emit();
  }
}
