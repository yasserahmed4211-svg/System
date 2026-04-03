import {
  Component, OnInit, OnDestroy, inject, effect, untracked,
  ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, signal, computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SharedModule } from 'src/app/theme/shared/shared.module';

import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';
import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { ErpLookupFieldComponent } from 'src/app/shared/components/erp-lookup-field/erp-lookup-field.component';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

import { LookupService } from 'src/app/core/services';
import { LookupSelectOption } from 'src/app/core/models';
import { LookupConfig, LookupItem } from 'src/app/core/lookup/lookup.model';
import { JournalFacade } from 'src/app/modules/finance/gl/facades/journal.facade';
import { JournalApiService } from 'src/app/modules/finance/gl/services/journal-api.service';
import {
  GlJournalHdrDto,
  GlJournalLineDto,
  ManualCreateJournalRequest,
  ManualUpdateJournalRequest,
  ManualJournalLineRequest,
  JournalLineRequest,
  UpdateJournalRequest,
  JOURNAL_LOOKUP_KEYS,
  JOURNAL_STATUS,
  JOURNAL_TYPE
} from 'src/app/modules/finance/gl/models/journal.model';

import { TranslateModule, TranslateService } from '@ngx-translate/core';

/**
 * JournalsFormComponent – GL Journal Create / Edit / View page.
 *
 * Header section with journal metadata fields,
 * plus dynamic lines table for debit/credit entries.
 */
@Component({
  selector: 'app-journals-form',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    TranslateModule,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpActionBarComponent,
    ErpEmptyStateComponent,
    ErpLookupFieldComponent
  ],
  templateUrl: './journals-form.component.html',
  styleUrl: './journals-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [JournalFacade, JournalApiService]
})
export class JournalsFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(JournalFacade);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);
  private readonly lookupService = inject(LookupService);

  constructor() {
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
  journalTypeOptions: LookupSelectOption[] = [];
  statusOptions: LookupSelectOption[] = [];
  sourceModuleOptions: LookupSelectOption[] = [];

  // ── Mode Detection ────────────────────────────────────────
  readonly isEditMode = signal(false);
  readonly isViewMode = signal(false);
  readonly journalId = signal<number | null>(null);
  readonly pageTitle = signal('');
  readonly submitPermission = signal('');
  readonly currentJournal = signal<GlJournalHdrDto | null>(null);

  // ── Form ──────────────────────────────────────────────────
  form!: FormGroup;
  get linesArray(): FormArray { return this.form.get('lines') as FormArray; }
  showValidationErrors = false;

  get isSaving(): boolean { return this.facade.saving(); }
  get saveError(): string | null { return this.facade.saveError(); }

  /** Whether the journal can be edited (only DRAFT journals) */
  readonly isEditable = computed(() => {
    if (this.isViewMode()) return false;
    if (!this.isEditMode()) return true; // create mode
    return this.currentJournal()?.statusIdFk === JOURNAL_STATUS.DRAFT;
  });

  /** Whether the journal is AUTOMATIC (generated from posting) */
  readonly isAutomatic = computed(() => {
    return this.currentJournal()?.journalTypeIdFk === JOURNAL_TYPE.AUTOMATIC;
  });

  // ── Computed totals (signal-based for OnPush reactivity) ──
  totalDebit = signal(0);
  totalCredit = signal(0);
  isBalanced = computed(() => Math.abs(this.totalDebit() - this.totalCredit()) < 0.001);

  private recalculateTotals(): void {
    let debit = 0;
    let credit = 0;
    for (let i = 0; i < this.linesArray.length; i++) {
      const line = this.linesArray.at(i);
      const d = line.get('debitAmount')?.value;
      const c = line.get('creditAmount')?.value;
      if (d) debit += +d;
      if (c) credit += +c;
    }
    this.totalDebit.set(debit);
    this.totalCredit.set(credit);
    this.cdr.markForCheck();
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const url = this.route.snapshot.url.map(s => s.path).join('/');
    const viewMode = url.includes('view');
    const editMode = !!idParam;

    this.isViewMode.set(viewMode);
    this.isEditMode.set(editMode);
    this.journalId.set(idParam ? +idParam : null);

    if (viewMode) {
      if (!this.authService.hasPermission('PERM_GL_JOURNAL_VIEW')) {
        this.notificationService.warning('MESSAGES.NO_PERMISSION');
        this.router.navigate(['/finance/gl/journals']);
        return;
      }
      this.pageTitle.set('GL.VIEW_JOURNAL');
      this.submitPermission.set('');
    } else if (editMode) {
      if (!this.authService.hasPermission('PERM_GL_JOURNAL_UPDATE')) {
        this.notificationService.warning('MESSAGES.NO_PERMISSION');
        this.router.navigate(['/finance/gl/journals']);
        return;
      }
      this.pageTitle.set('GL.EDIT_JOURNAL');
      this.submitPermission.set('PERM_GL_JOURNAL_UPDATE');
    } else {
      if (!this.authService.hasPermission('PERM_GL_JOURNAL_CREATE')) {
        this.notificationService.warning('MESSAGES.NO_PERMISSION');
        this.router.navigate(['/finance/gl/journals']);
        return;
      }
      this.pageTitle.set('GL.ADD_JOURNAL');
      this.submitPermission.set('PERM_GL_JOURNAL_CREATE');
    }

    this.buildForm();

    if (viewMode || editMode) {
      this.loadJournal();
    } else {
      this.addLine(); // start with one empty line
    }

    this.loadLookups();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadLookups());
  }

  private loadLookups(): void {
    const lang = this.translate.currentLang || 'ar';
    this.lookupService.getOptions(JOURNAL_LOOKUP_KEYS.JOURNAL_TYPE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.journalTypeOptions = opts; this.cdr.markForCheck(); });
    this.lookupService.getOptions(JOURNAL_LOOKUP_KEYS.JOURNAL_STATUS, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.statusOptions = opts; this.cdr.markForCheck(); });
    this.lookupService.getOptions(JOURNAL_LOOKUP_KEYS.SOURCE_MODULE, lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(opts => { this.sourceModuleOptions = opts; this.cdr.markForCheck(); });
  }

  // ── Form Construction ─────────────────────────────────────

  private buildForm(): void {
    this.form = this.fb.group({
      journalDate: [null, [Validators.required]],
      journalTypeIdFk: [{ value: JOURNAL_TYPE.MANUAL, disabled: true }],
      description: [null],
      sourceModuleIdFk: [{ value: null, disabled: true }],
      sourceDocTypeId: [{ value: null, disabled: true }],
      sourceDocIdFk: [{ value: null, disabled: true }],
      sourcePostingIdFk: [{ value: null, disabled: true }],
      lines: this.fb.array([], [Validators.required])
    });
  }

  private createLine(data?: Partial<JournalLineRequest & { accountCode?: string; accountName?: string }>): FormGroup {
    const group = this.fb.group({
      accountIdFk: [data?.accountIdFk ?? null, [Validators.required]],
      accountCode: [data?.accountCode ?? null],
      accountName: [data?.accountName ?? null],
      debitAmount: [data?.debitAmount ?? null],
      creditAmount: [data?.creditAmount ?? null],
      customerIdFk: [{ value: data?.customerIdFk ?? null, disabled: true }],
      supplierIdFk: [{ value: data?.supplierIdFk ?? null, disabled: true }],
      costCenterIdFk: [{ value: data?.costCenterIdFk ?? null, disabled: true }],
      description: [data?.description ?? null]
    });
    this.setupLineXorLogic(group);
    return group;
  }

  addLine(): void {
    this.linesArray.push(this.createLine());
    this.recalculateTotals();
    this.cdr.detectChanges();
  }

  removeLine(index: number): void {
    if (this.linesArray.length <= 1) {
      this.notificationService.warning('GL.MIN_ONE_LINE');
      return;
    }
    this.linesArray.removeAt(index);
    this.recalculateTotals();
    this.cdr.detectChanges();
  }

  // ── Load Journal for Edit / View ──────────────────────────

  private loadJournal(): void {
    const id = this.journalId();
    if (!id) return;

    this.facade.getJournalById(id, (journal: GlJournalHdrDto) => {
      this.currentJournal.set(journal);
      this.patchForm(journal);
      if (this.isViewMode() || !this.isEditable()) {
        this.form.disable();
      } else if (this.isAutomatic()) {
        // AUTOMATIC journals: only description + journalDate are editable
        this.form.disable();
        this.form.get('journalDate')?.enable();
        this.form.get('description')?.enable();
      }
      this.cdr.detectChanges();
    });
  }

  private patchForm(journal: GlJournalHdrDto): void {
    this.form.patchValue({
      journalDate: journal.journalDate,
      journalTypeIdFk: journal.journalTypeIdFk,
      description: journal.description,
      sourceModuleIdFk: journal.sourceModuleIdFk,
      sourceDocTypeId: journal.sourceDocTypeId,
      sourceDocIdFk: journal.sourceDocIdFk,
      sourcePostingIdFk: journal.sourcePostingIdFk
    });

    // Build lines
    this.linesArray.clear();
    if (journal.lines && journal.lines.length > 0) {
      for (const line of journal.lines) {
        this.linesArray.push(this.createLine({
          accountIdFk: line.accountIdFk,
          accountCode: line.accountCode ?? undefined,
          accountName: line.accountName ?? undefined,
          debitAmount: line.debitAmount,
          creditAmount: line.creditAmount,
          customerIdFk: line.customerIdFk,
          supplierIdFk: line.supplierIdFk,
          costCenterIdFk: line.costCenterIdFk,
          description: line.description
        }));
      }
    } else {
      this.addLine();
    }
    this.recalculateTotals();
  }

  // ── Account Lookup Callback ───────────────────────────────

  onAccountSelected(lineIndex: number, item: LookupItem): void {
    const line = this.linesArray.at(lineIndex);
    line.patchValue({
      accountCode: (item['code'] as string) ?? null,
      accountName: (item['name'] as string) ?? null
    });
  }

  // ── Line Debit/Credit XOR Logic ─────────────────────────

  private setupLineXorLogic(group: FormGroup): void {
    const debitCtrl = group.get('debitAmount')!;
    const creditCtrl = group.get('creditAmount')!;

    debitCtrl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(val => {
      if (val && +val > 0 && creditCtrl.value) {
        creditCtrl.setValue(null, { emitEvent: false });
      }
      this.recalculateTotals();
    });

    creditCtrl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(val => {
      if (val && +val > 0 && debitCtrl.value) {
        debitCtrl.setValue(null, { emitEvent: false });
      }
      this.recalculateTotals();
    });
  }

  // ── Submit ────────────────────────────────────────────────

  onSubmit(): void {
    this.showValidationErrors = true;
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const rawValue = this.form.getRawValue();
    const journalId = this.journalId();

    // AUTOMATIC journal update: only send header fields + existing lines unchanged
    if (this.isEditMode() && journalId && this.isAutomatic()) {
      const journal = this.currentJournal()!;
      const payload: UpdateJournalRequest = {
        journalDate: rawValue.journalDate,
        journalTypeIdFk: journal.journalTypeIdFk,
        description: rawValue.description ?? null,
        sourceModuleIdFk: journal.sourceModuleIdFk,
        sourceDocTypeId: journal.sourceDocTypeId,
        sourceDocIdFk: journal.sourceDocIdFk,
        sourcePostingIdFk: journal.sourcePostingIdFk,
        lines: (journal.lines ?? []).map(l => ({
          accountIdFk: l.accountIdFk,
          debitAmount: l.debitAmount,
          creditAmount: l.creditAmount,
          customerIdFk: l.customerIdFk,
          supplierIdFk: l.supplierIdFk,
          costCenterIdFk: l.costCenterIdFk,
          description: l.description
        }))
      };
      this.facade.updateJournalGeneral(journalId, payload, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.navigateBack();
      });
      return;
    }

    if (this.linesArray.length === 0) {
      this.notificationService.warning('GL.MIN_ONE_LINE');
      return;
    }

    if (!this.isBalanced()) {
      this.notificationService.warning('GL.JOURNAL_NOT_BALANCED');
      return;
    }

    const linesPayload: ManualJournalLineRequest[] = rawValue.lines.map((l: any) => ({
      accountIdFk: l.accountIdFk,
      debitAmount: l.debitAmount ?? null,
      creditAmount: l.creditAmount ?? null,
      description: l.description ?? null
    }));

    if (this.isEditMode() && journalId) {
      const payload: ManualUpdateJournalRequest = {
        journalDate: rawValue.journalDate,
        description: rawValue.description ?? null,
        lines: linesPayload
      };

      this.facade.updateJournal(journalId, payload, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.navigateBack();
      });
    } else {
      const payload: ManualCreateJournalRequest = {
        journalDate: rawValue.journalDate,
        description: rawValue.description ?? null,
        lines: linesPayload
      };

      this.facade.createJournal(payload, (created: GlJournalHdrDto) => {
        this.notificationService.success('MESSAGES.CREATE_SUCCESS');
        this.location.replaceState('/finance/gl/journals/edit/' + created.id);
        this.isEditMode.set(true);
        this.journalId.set(created.id);
        this.currentJournal.set(created);
        this.cdr.markForCheck();
      });
    }
  }

  navigateBack(): void {
    this.router.navigate(['/finance/gl/journals']);
  }

  // ── Template Helpers ──────────────────────────────────────

  isFieldInvalid(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!ctrl && ctrl.invalid && (ctrl.touched || this.showValidationErrors);
  }

  isLineFieldInvalid(lineIndex: number, fieldName: string): boolean {
    const ctrl = this.linesArray.at(lineIndex)?.get(fieldName);
    return !!ctrl && ctrl.invalid && (ctrl.touched || this.showValidationErrors);
  }

  getStatusLabel(): string {
    const journal = this.currentJournal();
    if (!journal) return '';
    const opt = this.statusOptions.find(o => o.value === journal.statusIdFk);
    return opt?.label ?? journal.statusIdFk ?? '';
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
