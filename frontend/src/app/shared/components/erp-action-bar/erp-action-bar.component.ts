import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'erp-action-bar',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './erp-action-bar.component.html',
  styleUrl: './erp-action-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpActionBarComponent {
  @Input() saveKey = 'COMMON.SAVE';
  @Input() cancelKey = 'COMMON.CANCEL';
  @Input() loadingKey = 'COMMON.SAVING';
  @Input() showSave = true;
  @Input() showCancel = true;
  @Input() showBack = true;
  @Input() loading = false;
  @Input() disabled = false;

  @Output() saveClicked = new EventEmitter<void>();
  @Output() cancelClicked = new EventEmitter<void>();
  @Output() backClicked = new EventEmitter<void>();

  protected onSave(): void {
    if (!this.loading && !this.disabled) {
      this.saveClicked.emit();
    }
  }

  protected onCancel(): void {
    if (!this.loading) {
      this.cancelClicked.emit();
    }
  }

  protected onBack(): void {
    this.backClicked.emit();
  }
}
