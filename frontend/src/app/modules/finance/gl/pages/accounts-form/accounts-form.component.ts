import { Component, OnInit, OnDestroy, inject, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';

import { GlFacade } from 'src/app/modules/finance/gl/facades/gl.facade';
import { GlApiService } from 'src/app/modules/finance/gl/services/gl-api.service';
import {
  AccountChartDto,
  CreateAccountRequest,
  UpdateAccountRequest,
  GL_LOOKUP_KEYS
} from 'src/app/modules/finance/gl/models/gl.model';
import { ParentAccountModalComponent } from 'src/app/modules/finance/gl/components/parent-account-modal/parent-account-modal.component';

import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LookupService } from 'src/app/core/services/lookup.service';
import { LookupDetail, LookupSelectOption } from 'src/app/core/models/lookup-detail.model';
import { confirmParentChange, confirmMakeRoot, GlConfirmActionDeps } from '../../helpers/gl-confirm-actions';

/**
 * AccountsFormComponent (Blueprint Level 2 – Page B: Form)
 *
 * @requirement FE-REQ-GL-001 §3.4
 * @task TASK-FE-GL-001
 */
@Component({
  selector: 'app-accounts-form',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    FormsModule,
    ReactiveFormsModule,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpActionBarComponent,
    TranslateModule
  ],
  templateUrl: './accounts-form.component.html',
  styleUrl: './accounts-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [GlFacade, GlApiService]
})
export class AccountsFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(GlFacade);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly dialogService = inject(ErpDialogService);
  private readonly authService = inject(AuthenticationService);
  private readonly modalService = inject(NgbModal);
  private readonly lookupService = inject(LookupService);

  private readonly PERM_CREATE = 'PERM_GL_ACCOUNT_CREATE';
  private readonly PERM_UPDATE = 'PERM_GL_ACCOUNT_UPDATE';

  accountForm!: FormGroup;
  isEditMode = false;
  accountPk: number | null = null;
  loading = false;

  /** Auto-generated account number (read-only, shown in edit mode) */
  generatedAccountNo: string | null = null;

  /** Label of the currently selected parent account (shown in UI) */
  selectedParentLabel: string | null = null;

  /** Parent account PK for clickable link */
  selectedParentPk: number | null = null;

  /** Breadcrumb path for hierarchy navigation */
  breadcrumb: Array<{ pk: number | null; name: string }> = [];

  /** Loaded account data for edit mode */
  currentAccount: AccountChartDto | null = null;

  /** Original parent FK to detect parent changes */
  private originalParentFk: number | null = null;

  /** Original account type to detect type changes */
  private originalAccountType: string | null = null;

  /** Whether account type change warning is shown */
  accountTypeChangeWarning = false;

  /** True when account has a parent — type is inherited and read-only */
  get isAccountTypeInherited(): boolean {
    return this.selectedParentPk !== null;
  }

  /**
   * Resolved label for the inherited account type (for read-only display).
   * Resolves from raw details using the currently-active language at call time.
   */
  get inheritedAccountTypeLabel(): string {
    const typeValue = this.accountForm.getRawValue().accountType as string | null;
    if (!typeValue) return '';
    const detail = this.accountTypeDetails.find(d => d.code === typeValue);
    if (detail) {
      return this.translate.currentLang === 'ar' ? detail.nameAr : (detail.nameEn || detail.nameAr);
    }
    const option = this.accountTypeOptions.find(o => o.value === typeValue);
    return option ? option.label : typeValue;
  }

  /** Dynamic account type options loaded from backend */
  accountTypeOptions: LookupSelectOption[] = [];
  /** Raw lookup details — language-safe label resolution in getters */
  private accountTypeDetails: LookupDetail[] = [];

  constructor() {
    this.initForm();

    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });
  }

  ngOnInit(): void {
    // Load account type options dynamically using raw details for language-safe label resolution
    this.lookupService.getLookup(GL_LOOKUP_KEYS.ACCOUNT_TYPE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(details => {
        this.accountTypeDetails = details;
        const lang = this.translate.currentLang || 'ar';
        this.accountTypeOptions = details.map(d => ({
          value: d.code,
          label: lang === 'ar' ? d.nameAr : (d.nameEn || d.nameAr)
        }));
        this.cdr.markForCheck();
      });

    // Rebuild option labels when the UI language changes
    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ lang }) => {
        this.accountTypeOptions = this.accountTypeDetails.map(d => ({
          value: d.code,
          label: lang === 'ar' ? d.nameAr : (d.nameEn || d.nameAr)
        }));
        this.cdr.markForCheck();
      });

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idParam = params.get('id');
        if (idParam) {
          this.isEditMode = true;
          this.accountPk = Number(idParam);

          if (!this.authService.hasPermission(this.PERM_UPDATE)) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.loadAccountForEdit(this.accountPk);
        } else {
          if (!this.authService.hasPermission(this.PERM_CREATE)) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.isEditMode = false;
          this.accountPk = null;
          this.setupCreateMode();
        }
        this.cdr.detectChanges();
      });
  }

  private initForm(): void {
    this.accountForm = this.fb.group({
      accountChartName: ['', [Validators.required]],
      accountType: [null, [Validators.required]],
      accountChartFk: [null],
      organizationFk: [null, [Validators.required]],
      organizationSubFk: [null],
      isActive: [true]
    });
  }

  private setupCreateMode(): void {
    this.generatedAccountNo = null;
    this.selectedParentLabel = null;
    this.accountForm.reset({
      accountChartName: '',
      accountType: null,
      accountChartFk: null,
      organizationFk: null,
      organizationSubFk: null,
      isActive: true
    });
    this.accountForm.enable();
  }

  private loadAccountForEdit(pk: number): void {
    this.loading = true;
    this.facade.getAccountById(pk, (account) => {
      if (account) {
        this.currentAccount = account;
        this.generatedAccountNo = account.accountChartNo;
        this.originalParentFk = account.accountChartFk;
        this.originalAccountType = account.accountType;
        this.accountForm.patchValue({
          accountChartName: account.accountChartName,
          accountType: account.accountType,
          accountChartFk: account.accountChartFk,
          organizationFk: account.organizationFk,
          organizationSubFk: account.organizationSubFk,
          isActive: account.isActive
        });
        this.accountForm.enable();
        // Organization is immutable after creation
        this.accountForm.get('organizationFk')?.disable();
        // Account type is inherited from parent and cannot be changed for child accounts
        if (account.accountChartFk) {
          this.accountForm.get('accountType')?.disable();
        }

        // Build parent label if parent is set
        if (account.accountChartFk && account.parentAccountName) {
          this.selectedParentPk = account.accountChartFk;
          this.selectedParentLabel = account.parentAccountNo
            ? `${account.parentAccountNo} - ${account.parentAccountName}`
            : account.parentAccountName;
        } else {
          this.selectedParentPk = null;
          this.selectedParentLabel = null;
        }

        // Build breadcrumb
        this.buildBreadcrumb(account);
      } else {
        this.notificationService.error('GL.ACCOUNT_NOT_FOUND');
        this.navigateBack();
      }
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  /**
   * Builds a simple breadcrumb from available account data.
   * Shows: Root (account type) > Parent > Current Account
   */
  private buildBreadcrumb(account: AccountChartDto): void {
    this.breadcrumb = [];

    // Add account type as the root crumb (resolve label from lookup options)
    if (account.accountType) {
      const typeOption = this.accountTypeOptions.find(o => o.value === account.accountType);
      const typeName = typeOption ? typeOption.label : account.accountType;
      this.breadcrumb.push({ pk: null, name: typeName });
    }

    // Add parent if exists
    if (account.parentAccountName) {
      this.breadcrumb.push({
        pk: account.accountChartFk,
        name: account.parentAccountNo
          ? `${account.parentAccountNo} - ${account.parentAccountName}`
          : account.parentAccountName
      });
    }

    // Add current account
    this.breadcrumb.push({
      pk: account.accountChartPk,
      name: account.accountChartNo
        ? `${account.accountChartNo} - ${account.accountChartName}`
        : account.accountChartName
    });
  }

  // ── Form Actions ──────────────────────────────────────────

  onSave(): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const formValue = this.accountForm.getRawValue();

    if (this.isEditMode && this.accountPk) {
      const updateRequest: UpdateAccountRequest = {
        accountChartName: formValue.accountChartName,
        accountType: formValue.accountType,
        accountChartFk: formValue.accountChartFk ?? undefined,
        organizationFk: formValue.organizationFk,
        organizationSubFk: formValue.organizationSubFk ?? undefined,
        isActive: formValue.isActive
      };

      this.facade.updateAccount(this.accountPk, updateRequest, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.navigateBack();
      });
    } else {
      const createRequest: CreateAccountRequest = {
        accountChartName: formValue.accountChartName,
        accountType: formValue.accountType,
        accountChartFk: formValue.accountChartFk ?? undefined,
        organizationFk: formValue.organizationFk,
        organizationSubFk: formValue.organizationSubFk ?? undefined,
        isActive: formValue.isActive
      };

      this.facade.createAccount(createRequest, () => {
        this.notificationService.success('MESSAGES.CREATE_SUCCESS');
        this.navigateBack();
      });
    }
  }

  onCancel(): void {
    this.navigateBack();
  }

  private navigateBack(): void {
    this.router.navigate(['/finance/gl/accounts']);
  }

  get pageTitleKey(): string {
    return this.isEditMode ? 'GL.EDIT_ACCOUNT' : 'GL.ADD_ACCOUNT';
  }

  // ── Account Type Change Safety ─────────────────────────────

  /**
   * Shows warning if account type is changed in edit mode (could have children).
   */
  onAccountTypeChange(): void {
    if (this.isEditMode && this.originalAccountType !== null) {
      const currentType = this.accountForm.get('accountType')?.value;
      this.accountTypeChangeWarning = currentType !== this.originalAccountType;
      this.cdr.detectChanges();
    }
  }

  // ── Parent Account Selection (Modal) ──────────────────────

  /**
   * Resets parent selection when organization changes in create mode.
   */
  onOrganizationChange(): void {
    this.accountForm.patchValue({ accountChartFk: null });
    this.selectedParentLabel = null;
    this.selectedParentPk = null;
    this.cdr.detectChanges();
  }

  /**
   * Opens the parent account selection modal.
   * Requires organizationFk to be set first.
   * Shows confirmation if changing parent in edit mode.
   */
  openParentModal(): void {
    const orgFk = this.accountForm.get('organizationFk')?.value;
    if (!orgFk) {
      this.notificationService.warning('GL.SELECT_ORG_FIRST');
      return;
    }

    const modalRef = this.modalService.open(ParentAccountModalComponent, {
      size: 'lg',
      centered: true,
      scrollable: true
    });

    modalRef.componentInstance.organizationFk = orgFk;
    modalRef.componentInstance.excludeAccountPk = this.isEditMode && this.accountPk ? this.accountPk : null;
    modalRef.componentInstance.currentParentPk = this.accountForm.get('accountChartFk')?.value ?? null;

    modalRef.result.then(
      (result: { parentPk: number | null; parentLabel: string | null; accountType?: string | null }) => {
        // If edit mode and parent changed, show confirmation
        if (this.isEditMode && this.originalParentFk !== result.parentPk) {
          const deps: GlConfirmActionDeps = {
            dialog: this.dialogService,
            notify: this.notificationService,
            auth: this.authService,
            facade: this.facade
          };
          confirmParentChange(deps, () => this.applyParentSelection(result));
        } else {
          this.applyParentSelection(result);
        }
      },
      () => { /* dismissed — no action */ }
    );
  }

  private applyParentSelection(result: { parentPk: number | null; parentLabel: string | null; accountType?: string | null }): void {
    this.accountForm.patchValue({ accountChartFk: result.parentPk });
    this.selectedParentLabel = result.parentLabel;
    this.selectedParentPk = result.parentPk;
    if (result.parentPk !== null) {
      // Inherit and lock the account type from the selected parent
      if (result.accountType) {
        this.accountForm.get('accountType')?.setValue(result.accountType);
      }
      this.accountForm.get('accountType')?.disable();
      this.accountTypeChangeWarning = false;
    } else {
      // Made root — re-enable account type selection
      this.accountForm.get('accountType')?.enable();
    }
    this.cdr.detectChanges();
  }

  /**
   * Clears the parent selection (makes it a root account).
   */
  clearParentSelection(): void {
    if (this.isEditMode && this.originalParentFk) {
      const deps: GlConfirmActionDeps = {
        dialog: this.dialogService,
        notify: this.notificationService,
        auth: this.authService,
        facade: this.facade
      };
      confirmMakeRoot(deps, () => {
        this.accountForm.patchValue({ accountChartFk: null });
        this.selectedParentLabel = null;
        this.selectedParentPk = null;
        this.accountForm.get('accountType')?.enable();
        this.cdr.detectChanges();
      });
    } else {
      this.accountForm.patchValue({ accountChartFk: null });
      this.selectedParentLabel = null;
      this.selectedParentPk = null;
      this.accountForm.get('accountType')?.enable();
      this.cdr.detectChanges();
    }
  }

  /**
   * Navigate to the tree view for this account
   */
  navigateToTree(): void {
    this.router.navigate(['/finance/gl/accounts/tree']);
  }

  /**
   * Navigate to edit the parent account
   */
  navigateToParent(): void {
    if (this.selectedParentPk) {
      this.router.navigate(['/finance/gl/accounts/edit', this.selectedParentPk]);
    }
  }

  /**
   * Navigate to a breadcrumb item
   */
  navigateToBreadcrumb(item: { pk: number | null; name: string }): void {
    if (item.pk && item.pk !== this.accountPk) {
      this.router.navigate(['/finance/gl/accounts/edit', item.pk]);
    }
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
