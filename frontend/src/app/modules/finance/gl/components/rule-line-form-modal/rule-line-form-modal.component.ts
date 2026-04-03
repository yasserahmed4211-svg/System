import {
  Component, Input, Output, EventEmitter, inject,
  ChangeDetectionStrategy, ViewChild, TemplateRef, DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpLookupFieldComponent } from 'src/app/shared/components/erp-lookup-field/erp-lookup-field.component';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { LookupSelectOption } from 'src/app/core/models';
import { LookupConfig } from 'src/app/core/lookup/lookup.model';
import { RuleLineRequest, AccRuleLineDto } from '../../models/gl.model';

/** Payload emitted when the user saves a line from the modal. */
export interface RuleLineSaveEvent {
  isEdit: boolean;
  line: RuleLineRequest & { accountCode?: string; accountName?: string };
  /** Index of the line being edited (only for isEdit=true). */
  editIndex?: number;
  modalRef: NgbModalRef;
}

/**
 * Self-contained modal form for adding / editing a single rule line.
 * Follows the master-lookups LookupDetailFormModal canonical pattern.
 *
 * @requirement FE-REQ-GL-001 §4.4
 */
@Component({
  selector: 'app-rule-line-form-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslateModule,
    ErpFormFieldComponent,
    ErpLookupFieldComponent
  ],
  templateUrl: './rule-line-form-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RuleLineFormModalComponent {
  private fb = inject(FormBuilder);
  private modalService = inject(NgbModal);
  private notificationService = inject(ErpNotificationService);
  private destroyRef = inject(DestroyRef);

  // ── Inputs (lookup options from the parent) ───────────────
  @Input() accountLookupConfig!: LookupConfig;
  @Input() entrySideOptions: LookupSelectOption[] = [];
  @Input() amountSourceTypeOptions: LookupSelectOption[] = [];
  @Input() entityTypeOptions: LookupSelectOption[] = [];
  @Input() paymentTypeOptions: LookupSelectOption[] = [];
  @Input() saving = false;

  @Output() saveLine = new EventEmitter<RuleLineSaveEvent>();

  @ViewChild('lineModal') lineModalRef!: TemplateRef<unknown>;
  @ViewChild(ErpLookupFieldComponent) accountLookupField!: ErpLookupFieldComponent;

  lineForm!: FormGroup;
  editingLine: AccRuleLineDto | null = null;
  editingIndex: number | null = null;
  private modalInstance: NgbModalRef | null = null;

  constructor() {
    this.initForm();
  }

  // ── Form ──────────────────────────────────────────────────

  private initForm(): void {
    this.lineForm = this.fb.group({
      accountIdFk: [null, [Validators.required]],
      entrySide: [null, [Validators.required]],
      priority: [1, [Validators.required, Validators.min(1)]],
      amountSourceType: [null, [Validators.required]],
      amountSourceValue: [null],
      paymentTypeCode: [null],
      entityType: [null]
    });

    // Conditional validation for amountSourceValue
    this.lineForm.get('amountSourceType')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((type) => {
        const ctrl = this.lineForm.get('amountSourceValue')!;
        if (type === 'FIXED' || type === 'PERCENT') {
          ctrl.setValidators([Validators.required]);
        } else {
          ctrl.clearValidators();
        }
        ctrl.updateValueAndValidity();
      });
  }

  // ── Public API ────────────────────────────────────────────

  /**
   * Open the modal for creating a new line or editing an existing one.
   * @param line            existing line data (edit mode) or undefined (create mode)
   * @param index           index within the parent lines array (edit mode only)
   * @param presetEntrySide entry side preset from the dual-column add buttons
   */
  open(line?: AccRuleLineDto | RuleLineRequest, index?: number, presetEntrySide?: string): void {
    this.editingLine = (line as AccRuleLineDto) ?? null;
    this.editingIndex = index ?? null;

    // Re-enable all fields first (reset from previous open)
    this.lineForm.get('entrySide')!.enable();
    this.lineForm.get('priority')!.enable();

    if (line) {
      this.lineForm.patchValue({
        accountIdFk: line.accountIdFk,
        entrySide: line.entrySide,
        priority: line.priority ?? 1,
        amountSourceType: line.amountSourceType,
        amountSourceValue: line.amountSourceValue ?? null,
        paymentTypeCode: line.paymentTypeCode ?? null,
        entityType: line.entityType ?? null
      });
    } else {
      this.lineForm.reset({
        accountIdFk: null,
        entrySide: presetEntrySide ?? null,
        priority: 1,
        amountSourceType: null,
        amountSourceValue: null,
        paymentTypeCode: null,
        entityType: null
      });
    }

    // Lock entry side when preset from dual-column buttons or editing existing line
    if (presetEntrySide || line) {
      this.lineForm.get('entrySide')!.disable();
    }

    // Priority is always auto-calculated by the parent after save
    this.lineForm.get('priority')!.disable();

    this.modalInstance = this.modalService.open(this.lineModalRef, {
      backdrop: 'static',
      keyboard: false,
      centered: true,
      size: 'lg'
    });
  }

  // ── Handlers ──────────────────────────────────────────────

  onSave(): void {
    if (this.lineForm.invalid) {
      this.lineForm.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const v = this.lineForm.getRawValue();

    // Resolve account display info from the lookup field
    let accountCode = '';
    let accountName = '';
    if (this.accountLookupField) {
      const display = this.accountLookupField.displayText();
      if (display) {
        const sepIndex = display.indexOf(' - ');
        if (sepIndex > 0) {
          accountCode = display.substring(0, sepIndex).trim();
          accountName = display.substring(sepIndex + 3).trim();
        } else {
          accountName = display;
        }
      }
    }

    const linePayload: RuleLineRequest & { accountCode?: string; accountName?: string } = {
      accountIdFk: Number(v.accountIdFk),
      entrySide: v.entrySide,
      priority: Number(v.priority) || 1,
      amountSourceType: v.amountSourceType,
      amountSourceValue: v.amountSourceValue != null ? Number(v.amountSourceValue) : null,
      paymentTypeCode: v.paymentTypeCode || null,
      entityType: v.entityType || null,
      accountCode,
      accountName
    };

    this.saveLine.emit({
      isEdit: !!this.editingLine,
      line: linePayload,
      editIndex: this.editingIndex ?? undefined,
      modalRef: this.modalInstance!
    });
  }

  /** Whether the amountSourceValue field should be visible. */
  get showAmountValue(): boolean {
    const type = this.lineForm.get('amountSourceType')?.value;
    return type === 'FIXED' || type === 'PERCENT';
  }
}
