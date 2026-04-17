import {
  Component, OnInit, OnDestroy, inject, signal,
  ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, effect, untracked
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';
import { ErpLookupFieldComponent } from 'src/app/shared/components/erp-lookup-field/erp-lookup-field.component';
import { LookupConfig } from 'src/app/core/lookup/lookup.model';

import { BranchFacade } from '../../facades/branch.facade';
import { BranchApiService } from '../../services/branch-api.service';
import { DepartmentDto } from '../../models/branch.model';
import { BranchFormMapper } from '../../models/branch-form.model';
import { DepartmentsSectionComponent } from '../../components/departments-section/departments-section.component';
import { ConfirmActionDeps, confirmDeactivateDepartment } from '../../helpers/branch-confirm-actions';
import { LookupService } from 'src/app/core/services/lookup.service';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

@Component({
  selector: 'app-branch-entry',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    TranslateModule,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpActionBarComponent,
    ErpPermissionDirective,
    ErpLookupFieldComponent,
    DepartmentsSectionComponent
  ],
  providers: [BranchFacade, BranchApiService],
  templateUrl: './branch-entry.component.html',
  styleUrl: './branch-entry.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BranchEntryComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(BranchFacade);
  private readonly lookupService = inject(LookupService);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  private readonly confirmDeps: ConfirmActionDeps = {
    dialog: inject(ErpDialogService),
    notify: this.notificationService,
    auth: this.authService,
    facade: this.facade
  };

  readonly isEditMode = signal(false);
  readonly entityId = signal<number | null>(null);
  readonly loading = signal(false);

  /** Per-row validation errors for the department inline table */
  readonly deptRowErrors = signal<boolean[]>([]);
  /** Show the validation summary banner above the department table */
  readonly deptShowSummary = signal(false);

  form!: FormGroup;
  statusOptions: LookupSelectOption[] = [];
  branchTypeOptions: LookupSelectOption[] = [];
  departmentTypeOptions: LookupSelectOption[] = [];

  // LOV configs
  readonly legalEntityLookupConfig: LookupConfig = {
    endpoint: '/api/organization/lookups/legal-entities',
    mode: 'quick',
    dialogTitleKey: 'BRANCHES.SELECT_LEGAL_ENTITY',
    columns: [
      { key: 'code', label: 'COMMON.CODE' },
      { key: 'name', label: 'COMMON.NAME' }
    ]
  };

  // Region LOV config — initially no filter; updated when legalEntityFk selected
  regionLookupConfig = signal<LookupConfig>({
    endpoint: '/api/organization/lookups/regions',
    mode: 'quick',
    dialogTitleKey: 'BRANCHES.SELECT_REGION',
    columns: [
      { key: 'code', label: 'COMMON.CODE' },
      { key: 'name', label: 'COMMON.NAME' }
    ]
  });

  constructor() {
    this.form = this.buildForm();

    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });

    effect(() => {
      const deptError = this.facade.deptSaveError();
      if (!deptError) return;
      untracked(() => this.notificationService.error(deptError));
    });
  }

  ngOnInit(): void {
    this.loadLookups();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadLookups());

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idParam = params.get('id');
        if (idParam) {
          this.isEditMode.set(true);
          this.entityId.set(Number(idParam));

          if (!this.authService.hasPermission('PERM_BRANCH_UPDATE')) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.loadForEdit(this.entityId()!);
        } else {
          if (!this.authService.hasPermission('PERM_BRANCH_CREATE')) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.isEditMode.set(false);
          this.entityId.set(null);
          this.loadForCreate();
        }
        this.cdr.detectChanges();
      });
  }

  ngOnDestroy(): void { this.facade.clearCurrentEntity(); }

  private buildForm(): FormGroup {
    return this.fb.group({
      branchPk:        [{ value: null, disabled: true }],
      branchCode:      [{ value: '', disabled: true }],
      legalEntityFk:   [null, Validators.required],
      regionFk:        [null],
      branchNameAr:    ['', [Validators.required, Validators.maxLength(200)]],
      branchNameEn:    ['', [Validators.required, Validators.maxLength(200)]],
      branchTypeId:    ['', Validators.required],
      isHeadquarterFl: [0],
      statusId:        ['ACTIVE', Validators.required],
      addressLine1:    [''],
      addressLine2:    [''],
      cityName:        [''],
      phone:           [''],
      email:           ['', Validators.email]
    });
  }

  loadForCreate(): void {
    const empty = BranchFormMapper.createEmpty();
    this.form.reset(empty);
    this.form.enable();
    this.form.get('branchPk')?.disable();
    this.form.get('branchCode')?.disable();
  }

  loadForEdit(id: number): void {
    this.loading.set(true);
    this.facade.getById(id, (entity) => {
      if (!entity) {
        this.notificationService.error('MESSAGES.RECORD_NOT_FOUND');
        this.navigateBack();
        return;
      }

      const formModel = BranchFormMapper.fromDomain(entity);
      this.form.patchValue(formModel);
      this.form.enable();

      // Immutable fields after save
      this.form.get('branchPk')?.disable();
      this.form.get('branchCode')?.disable();
      this.form.get('legalEntityFk')?.disable();

      // Update region LOV to filter by the stored legalEntityFk
      if (entity.legalEntityFk) {
        this.updateRegionLovFilter(entity.legalEntityFk);
      }

      this.facade.getUsageInfo(id);
      this.loading.set(false);
      this.cdr.detectChanges();
    });
  }

  // ─── LOV Events ──────────────────────────────────────────────────────────────

  onLegalEntitySelected(item: { id: number } | null): void {
    if (!item) {
      this.form.get('regionFk')?.reset(null);
      this.regionLookupConfig.set({ ...this.regionLookupConfig(), extraParams: undefined });
      return;
    }
    // Clear region selection and filter LOV
    this.form.get('regionFk')?.reset(null);
    this.updateRegionLovFilter(item.id);
  }

  private updateRegionLovFilter(legalEntityId: number): void {
    this.regionLookupConfig.set({
      ...this.regionLookupConfig(),
      extraParams: { legalEntityFk: String(legalEntityId) }
    });
  }

  // ─── Department handlers ──────────────────────────────────────────────────────

  get departments() { return this.facade.departments(); }

  onAddDeptRow(): void {
    const newDept: DepartmentDto = {
      departmentPk: 0,
      departmentCode: '',
      branchFk: this.entityId() ?? 0,
      departmentNameAr: '',
      departmentNameEn: '',
      departmentTypeId: '',
      activeFl: 1
    };
    this.facade.addLocalDepartment(newDept);
    this.cdr.detectChanges();
  }

  onRemoveDeptRow(index: number): void {
    const dept = this.facade.departments()[index];
    if (dept && dept.departmentPk) {
      // Saved row → confirm + deactivate via API
      confirmDeactivateDepartment(this.confirmDeps, dept);
    } else {
      // Unsaved row → remove locally without API call
      this.facade.removeLocalDepartment(index);
    }
    this.cdr.detectChanges();
  }

  onDeactivateDeptRow(dept: DepartmentDto): void {
    confirmDeactivateDepartment(this.confirmDeps, dept);
  }

  onDeptFieldChange(event: { index: number; field: keyof DepartmentDto; value: unknown }): void {
    const dept = this.facade.departments()[event.index];
    if (dept?.departmentPk) {
      // Saved row → update via API immediately
      this.facade.updateDepartment(dept.departmentPk, {
        departmentNameAr: event.field === 'departmentNameAr' ? event.value as string : dept.departmentNameAr,
        departmentNameEn: event.field === 'departmentNameEn' ? event.value as string : dept.departmentNameEn,
        departmentTypeId: event.field === 'departmentTypeId' ? event.value as string : dept.departmentTypeId
      } as any);
    } else {
      // Unsaved row → update local signal only
      this.facade.updateLocalDepartment(event.index, { [event.field]: event.value });
      // Clear row error for this row now that the user is editing
      const errs = [...this.deptRowErrors()];
      errs[event.index] = false;
      this.deptRowErrors.set(errs);
      if (errs.every(e => !e)) { this.deptShowSummary.set(false); }
    }
  }

  onSaveDepartments(): void {
    const depts = this.facade.departments();
    const unsaved = depts.map((d, i) => ({ dept: d, index: i })).filter(x => !x.dept.departmentPk);

    // Validate all unsaved rows — F3.4
    const errors = depts.map(d =>
      !d.departmentPk && (!d.departmentNameAr?.trim() || !d.departmentNameEn?.trim() || !d.departmentTypeId?.trim())
    );
    const hasErrors = errors.some(Boolean);

    this.deptRowErrors.set(errors);
    this.deptShowSummary.set(hasErrors);

    if (hasErrors) {
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      this.cdr.detectChanges();
      return;
    }

    // Save each unsaved row sequentially via the facade
    const toSave = unsaved.filter(x =>
      x.dept.departmentNameAr?.trim() && x.dept.departmentNameEn?.trim() && x.dept.departmentTypeId?.trim()
    );

    toSave.forEach(({ dept }) => {
      this.facade.createDepartment({
        branchFk: this.entityId()!,
        departmentNameAr: dept.departmentNameAr,
        departmentNameEn: dept.departmentNameEn,
        departmentTypeId: dept.departmentTypeId,
        activeFl: dept.activeFl
      });
    });

    this.deptRowErrors.set([]);
    this.deptShowSummary.set(false);
    this.cdr.detectChanges();
  }

  // ─── Save ─────────────────────────────────────────────────────────────────────

  get pageTitleKey(): string {
    return this.isEditMode() ? 'BRANCHES.EDIT' : 'BRANCHES.CREATE';
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const rawValue = this.form.getRawValue();

    if (this.isEditMode() && this.entityId()) {
      const request = BranchFormMapper.toUpdateRequest(rawValue);
      this.facade.update(this.entityId()!, request, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.navigateBack();
      });
    } else {
      const request = BranchFormMapper.toCreateRequest(rawValue);
      this.facade.create(request, () => {
        this.notificationService.success('MESSAGES.CREATE_SUCCESS');
        this.navigateBack();
      });
    }
  }

  private loadLookups(): void {
    const lang = this.translate.currentLang || 'ar';

    this.lookupService.getOptions('STATUS', lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((options) => {
        this.statusOptions = options;
        this.cdr.markForCheck();
      });

    this.lookupService.getOptions('BRANCH_TYPE', lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((options) => {
        this.branchTypeOptions = options;
        this.cdr.markForCheck();
      });

    this.lookupService.getOptions('DEPARTMENT_TYPE', lang)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((options) => {
        this.departmentTypeOptions = options;
        this.cdr.markForCheck();
      });
  }

  onCancel(): void { this.navigateBack(); }
  private navigateBack(): void { this.router.navigate(['/organization/branches']); }
}
