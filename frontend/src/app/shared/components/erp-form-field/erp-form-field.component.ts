import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

import { ErpUiMessageResolverService } from '../../services/erp-ui-message-resolver.service';
import { FormErrorResult } from '../../utils/form-error-resolver';

/**
 * ErpFormFieldComponent
 *
 * Pure UI wrapper for label + projected control + validation error.
 * Entity-agnostic; returns translation keys only.
 */
@Component({
  selector: 'erp-form-field',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './erp-form-field.component.html',
  styleUrls: ['./erp-form-field.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpFormFieldComponent implements OnChanges, OnDestroy {
  @Input() labelKey = '';
  @Input() control: AbstractControl | null = null;
  @Input() required = false;
  @Input() hintKey?: string;

  private controlSub = new Subscription();

  constructor(
    private readonly resolver: ErpUiMessageResolverService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ('control' in changes) {
      this.controlSub.unsubscribe();
      this.controlSub = new Subscription();

      if (this.control) {
        // Subscribe to control.events (Angular 18+) which emits ALL
        // state changes including TouchedChangeEvent from markAllAsTouched().
        // This replaces the old merge(statusChanges, valueChanges) which
        // missed touch state updates, causing error messages not to render.
        this.controlSub.add(
          this.control.events.subscribe(() => {
            this.cdr.markForCheck();
          })
        );
      }
    }
  }

  ngOnDestroy(): void {
    this.controlSub.unsubscribe();
  }

  get showError(): boolean {
    const ctrl = this.control;
    return !!(ctrl && ctrl.invalid && (ctrl.touched || ctrl.dirty) && this.error);
  }

  get error(): FormErrorResult | null {
    return this.resolver.resolveFormValidation(this.control);
  }
}
