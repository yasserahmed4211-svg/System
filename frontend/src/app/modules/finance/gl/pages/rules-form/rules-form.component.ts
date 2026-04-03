import {
  Component, OnInit, OnDestroy, inject, effect, untracked, signal,
  ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, ViewChild
} from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SharedModule } from 'src/app/theme/shared/shared.module';

import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';

import { RuleLineFormModalComponent, RuleLineSaveEvent } from '../../components/rule-line-form-modal/rule-line-form-modal.component';
import { RuleLinesSectionComponent, RuleLineRow } from '../../components/rule-lines-section/rule-lines-section.component';

import { LookupService } from 'src/app/core/services';
import { LookupSelectOption } from 'src/app/core/models';
import { LookupConfig } from 'src/app/core/lookup/lookup.model';
import { GlFacade } from 'src/app/modules/finance/gl/facades/gl.facade';
import { GlApiService } from 'src/app/modules/finance/gl/services/gl-api.service';
import {
  AccRuleHdrDto,
  CreateRuleRequest,
  UpdateRuleRequest,
  RuleLineRequest,
  GL_LOOKUP_KEYS
} from 'src/app/modules/finance/gl/models/gl.model';

import { TranslateModule, TranslateService } from '@ngx-translate/core';

function minimumLineItemsValidator(minimum: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!(control instanceof FormArray)) {
      return null;
    }

    return control.length >= minimum
      ? null
      : { minItems: { required: minimum, actual: control.length } };
  };
}

/**
 * RulesFormComponent (Blueprint Level 2 – Page B: Form/Detail)
 *
 * Header fields + dynamic lines table with add/remove row.
 *
 * @requirement FE-REQ-GL-001 §4.3 – §4.5
 * @task TASK-FE-GL-001
 */
@Component({
  selector: 'app-rules-form',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    TranslateModule,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpActionBarComponent,
    RuleLineFormModalComponent,
    RuleLinesSectionComponent
  ],
  templateUrl: './rules-form.component.html',
  styleUrl: './rules-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [GlFacade, GlApiService]
})
export class RulesFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(GlFacade);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly lookupService = inject(LookupService);

  private readonly location = inject(Location);

  constructor() {
    // Auto-display save errors as toast notifications (same pattern as accounts-form)
    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });
  }

  // ── Account Lookup Config ─────────────────────────────────
  readonly accountLookupConfig: LookupConfig = {
    endpoint: '/api/gl/accounts/lookup',
    mode: 'advanced',
    pageSize: 15,
    dialogTitleKey: 'GL.SELECT_ACCOUNT',
    placeholderKey: 'GL.SEARCH_ACCOUNT',
    columns: [
      { key: 'code', label: 'GL.ACCOUNT_CODE', width: '140px' },
      { key: 'name', label: 'GL.ACCOUNT_NAME' },
      { key: 'type', label: 'GL.ACCOUNT_TYPE', width: '140px' }
    ]
  };

  // ── Dynamic Lookups ───────────────────────────────────────
  sourceModuleOptions: LookupSelectOption[] = [];
  sourceDocTypeOptions: LookupSelectOption[] = [];
  entrySideOptions: LookupSelectOption[] = [];
  amountSourceTypeOptions: LookupSelectOption[] = [];
  entityTypeOptions: LookupSelectOption[] = [];
  paymentTypeOptions: LookupSelectOption[] = [];

  // ── Mode Detection ────────────────────────────────────────
  readonly isEditMode = signal(false);
  readonly ruleId = signal<number | null>(null);
  readonly pageTitle = signal('');
  readonly submitPermission = signal('');

  // ── Form ──────────────────────────────────────────────────
  form!: FormGroup;
  get debitLinesArray(): FormArray { return this.form.get('debitLines') as FormArray; }
  get creditLinesArray(): FormArray { return this.form.get('creditLines') as FormArray; }
  showValidationErrors = false;

  get isSaving(): boolean { return this.facade.saving(); }
  get saveError(): string | null { return this.facade.saveError(); }

  ngOnInit(): void {
    this.buildForm();

    const idParam = this.route.snapshot.paramMap.get('id');
    this.isEditMode.set(!!idParam);
    this.ruleId.set(idParam ? +idParam : null);

    if (this.isEditMode()) {
      this.pageTitle.set('GL.EDIT_RULE');
      this.submitPermission.set('PERM_GL_RULE_UPDATE');
      this.loadRule();
    } else {
      this.pageTitle.set('GL.ADD_RULE');
      this.submitPermission.set('PERM_GL_RULE_CREATE');
    }

    this.loadLookups();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadLookups());
  }

  private loadLookups(): void {
    const lang = this.translate.currentLang || 'ar';
    this.lookupService.getOptions(GL_LOOKUP_KEYS.SOURCE_MODULE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.sourceModuleOptions = opts; this.cdr.markForCheck(); });
    this.lookupService.getOptions(GL_LOOKUP_KEYS.SOURCE_DOC_TYPE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.sourceDocTypeOptions = opts; this.cdr.markForCheck(); });
    this.lookupService.getOptions(GL_LOOKUP_KEYS.ENTRY_SIDE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.entrySideOptions = opts; this.cdr.markForCheck(); });
    this.lookupService.getOptions(GL_LOOKUP_KEYS.AMOUNT_SOURCE_TYPE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.amountSourceTypeOptions = opts; this.cdr.markForCheck(); });
    this.lookupService.getOptions(GL_LOOKUP_KEYS.ENTITY_TYPE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.entityTypeOptions = opts; this.cdr.markForCheck(); });
    this.lookupService.getOptions(GL_LOOKUP_KEYS.PAYMENT_TYPE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.paymentTypeOptions = opts; this.cdr.markForCheck(); });
  }

  // ── Form Construction ─────────────────────────────────────

  private buildForm(): void {
    this.form = this.fb.group({
      companyId: [null, [Validators.required, Validators.min(1)]],
      sourceUnit: [null, [Validators.required]],
      documentType: [null, [Validators.required]],
      isActive: [true],
      debitLines: this.fb.array([], [minimumLineItemsValidator(1)]),
      creditLines: this.fb.array([], [minimumLineItemsValidator(1)])
    });
  }

  private createLine(entrySide: 'DEBIT' | 'CREDIT', data?: Partial<RuleLineRequest>): FormGroup {
    const line = this.fb.group({
      accountIdFk: [data?.accountIdFk ?? null, [Validators.required]],
      entrySide: [entrySide, [Validators.required]],
      priority: [data?.priority ?? 1, [Validators.required, Validators.min(1)]],
      amountSourceType: [data?.amountSourceType ?? null, [Validators.required]],
      amountSourceValue: [data?.amountSourceValue ?? null],
      paymentTypeCode: [data?.paymentTypeCode ?? null],
      entityType: [data?.entityType ?? null]
    });

    // Conditional validation for amountSourceValue
    line.get('amountSourceType')!.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((type) => {
        const valueCtrl = line.get('amountSourceValue')!;
        if (type === 'FIXED' || type === 'PERCENT') {
          valueCtrl.setValidators([Validators.required]);
        } else {
          valueCtrl.clearValidators();
        }
        valueCtrl.updateValueAndValidity();
      });

    return line;
  }

  private lineArrayForSide(side: 'DEBIT' | 'CREDIT'): FormArray {
    return side === 'DEBIT' ? this.debitLinesArray : this.creditLinesArray;
  }

  private linePayloadForSide(side: 'DEBIT' | 'CREDIT'): RuleLineRequest[] {
    return this.lineArrayForSide(side).controls.map((control) => {
      const value = (control as FormGroup).getRawValue();
      return {
        accountIdFk: value.accountIdFk,
        entrySide: side,
        priority: value.priority,
        amountSourceType: value.amountSourceType,
        amountSourceValue: value.amountSourceValue ?? null,
        paymentTypeCode: value.paymentTypeCode ?? null,
        entityType: value.entityType ?? null
      };
    });
  }

  private reindexLinePriorities(): void {
    let priority = 1;
    ['DEBIT', 'CREDIT'].forEach((side) => {
      this.lineArrayForSide(side as 'DEBIT' | 'CREDIT').controls.forEach((control) => {
        control.get('priority')?.setValue(priority++);
      });
    });
  }

  private removeLine(side: 'DEBIT' | 'CREDIT', index: number): void {
    this.lineArrayForSide(side).removeAt(index);
    this.reindexLinePriorities();
    this.cdr.detectChanges();
  }

  // ── Load Rule for Edit ────────────────────────────────────

  private loadRule(): void {
    const id = this.ruleId();
    if (!id) return;

    this.facade.getRuleById(id, (rule: AccRuleHdrDto) => {
      this.patchForm(rule);
      this.cdr.detectChanges();
    });
  }

  private patchForm(rule: AccRuleHdrDto): void {
    this.form.patchValue({
      companyId: rule.companyIdFk,
      sourceUnit: rule.sourceModule,
      documentType: rule.sourceDocType,
      isActive: rule.isActive
    });

    // Disable company in edit mode
    this.form.get('companyId')!.disable();

    // Build lines
    this.debitLinesArray.clear();
    this.creditLinesArray.clear();
    if (rule.lines && rule.lines.length > 0) {
      for (const line of rule.lines) {
        this.lineArrayForSide(line.entrySide === 'CREDIT' ? 'CREDIT' : 'DEBIT').push(this.createLine(
          line.entrySide === 'CREDIT' ? 'CREDIT' : 'DEBIT', {
          accountIdFk: line.accountIdFk,
          entrySide: line.entrySide,
          priority: line.priority,
          amountSourceType: line.amountSourceType,
          amountSourceValue: line.amountSourceValue,
          paymentTypeCode: line.paymentTypeCode,
          entityType: line.entityType
        }));
      }
    }

    this.reindexLinePriorities();
  }

  // ── Submit ────────────────────────────────────────────────

  onSubmit(): void {
    this.showValidationErrors = true;
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      this.scrollToFirstError();
      return;
    }

    const rawValue = this.form.getRawValue();
    const debitLines = this.linePayloadForSide('DEBIT');
    const creditLines = this.linePayloadForSide('CREDIT');

    if (this.isEditMode() && this.ruleId()) {
      const payload: UpdateRuleRequest = {
        companyId: rawValue.companyId,
        sourceUnit: rawValue.sourceUnit,
        documentType: rawValue.documentType,
        isActive: rawValue.isActive ?? true,
        debitLines,
        creditLines
      };

      this.facade.updateRule(this.ruleId()!, payload, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.navigateBack();
      });
    } else {
      const payload: CreateRuleRequest = {
        companyId: rawValue.companyId,
        sourceUnit: rawValue.sourceUnit,
        documentType: rawValue.documentType,
        isActive: rawValue.isActive ?? true,
        debitLines,
        creditLines
      };

      this.facade.createRule(payload, () => {
        this.notificationService.success('MESSAGES.CREATE_SUCCESS');
        this.navigateBack();
      });
    }
  }

  navigateBack(): void {
    this.router.navigate(['/finance/gl/rules']);
  }

  // ── Template Helpers ──────────────────────────────────────

  /** Derived debit lines from the FormArray for the dual-column display. */
  get debitLines(): RuleLineRow[] {
    return this.debitLinesArray.controls
      .map((control) => ({ ...(control as FormGroup).getRawValue() }));
  }

  /** Derived credit lines from the FormArray for the dual-column display. */
  get creditLines(): RuleLineRow[] {
    return this.creditLinesArray.controls
      .map((control) => ({ ...(control as FormGroup).getRawValue() }));
  }

  get validationSummaryKeys(): string[] {
    const keys: string[] = [];

    if (this.form.get('companyId')?.invalid) {
      keys.push('GL.COMPANY_ID_REQUIRED');
    }
    if (this.form.get('sourceUnit')?.invalid) {
      keys.push('GL.SOURCE_UNIT_REQUIRED');
    }
    if (this.form.get('documentType')?.invalid) {
      keys.push('GL.DOCUMENT_TYPE_REQUIRED');
    }
    if (this.debitLinesArray.invalid) {
      keys.push('GL.DEBIT_LINE_REQUIRED');
    }
    if (this.creditLinesArray.invalid) {
      keys.push('GL.CREDIT_LINE_REQUIRED');
    }

    return [...new Set(keys)];
  }

  @ViewChild(RuleLineFormModalComponent) lineModal!: RuleLineFormModalComponent;

  /** Open the modal to add a new line with a preset entry side. */
  openAddLineForSide(side: 'DEBIT' | 'CREDIT'): void {
    this.lineModal.open(undefined, undefined, side);
  }

  /** Open the modal to edit an existing line (from a sided section). */
  onEditLineSided(event: { line: RuleLineRow; index: number }): void {
    const side = event.line.entrySide === 'CREDIT' ? 'CREDIT' : 'DEBIT';
    this.lineModal.open(event.line as any, event.index, side);
  }

  /** Remove a line by its global FormArray index (resolved from sided index). */
  onRemoveLineSided(sidedIndex: number, side: 'DEBIT' | 'CREDIT'): void {
    this.removeLine(side, sidedIndex);
  }

  /** Handle the save event from the line modal. */
  onLineSaved(event: RuleLineSaveEvent): void {
    const side = event.line.entrySide === 'CREDIT' ? 'CREDIT' : 'DEBIT';
    const targetArray = this.lineArrayForSide(side);

    if (event.isEdit && event.editIndex != null) {
      const fg = targetArray.at(event.editIndex) as FormGroup;
      fg.patchValue(event.line);
    } else {
      targetArray.push(this.createLine(side, event.line));
    }

    this.reindexLinePriorities();
    event.modalRef.close();
    this.cdr.detectChanges();
  }

  isFieldInvalid(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!ctrl && ctrl.invalid && (ctrl.touched || this.showValidationErrors);
  }

  isArrayInvalid(name: 'debitLines' | 'creditLines'): boolean {
    const ctrl = this.form.get(name);
    return !!ctrl && ctrl.invalid && this.showValidationErrors;
  }

  private scrollToFirstError(): void {
    queueMicrotask(() => {
      const firstError = document.querySelector(
        '[data-validation-error="true"], .form-control.is-invalid, .form-select.is-invalid'
      ) as HTMLElement | null;

      firstError?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      firstError?.focus?.();
    });
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
