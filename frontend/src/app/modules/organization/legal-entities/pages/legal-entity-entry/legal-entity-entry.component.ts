import {
  Component, OnInit, OnDestroy, inject, signal, computed,
  ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, effect, untracked
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';
import { ErpLookupFieldComponent } from 'src/app/shared/components/erp-lookup-field/erp-lookup-field.component';
import { LookupConfig } from 'src/app/core/lookup/lookup.model';

import { LegalEntityFacade } from '../../facades/legal-entity.facade';
import { LegalEntityApiService } from '../../services/legal-entity-api.service';
import { LegalEntityFormMapper } from '../../models/legal-entity-form.model';
import { urlValidator } from 'src/app/shared/utils/form-error-resolver';
import { LookupService } from 'src/app/core/services/lookup.service';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

@Component({
  selector: 'app-legal-entity-entry',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    TranslateModule,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpActionBarComponent,
    ErpLookupFieldComponent
  ],
  providers: [LegalEntityFacade, LegalEntityApiService],
  templateUrl: './legal-entity-entry.component.html',
  styleUrl: './legal-entity-entry.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LegalEntityEntryComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(LegalEntityFacade);
  private readonly lookupService = inject(LookupService);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  readonly isEditMode = signal(false);
  readonly entityId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly isCurrencyLocked = computed(() => this.isEditMode() && !!this.facade.usageInfo()?.hasFinancialTransactions);

  form!: FormGroup;
  statusOptions: LookupSelectOption[] = [];

  readonly countryLookupConfig: LookupConfig = {
    endpoint: '/api/organization/lookups/countries',
    mode: 'quick',
    dialogTitleKey: 'LEGAL_ENTITIES.SELECT_COUNTRY',
    columns: [
      { key: 'code', label: 'COMMON.CODE' },
      { key: 'name', label: 'COMMON.NAME' }
    ]
  };

  readonly currencyLookupConfig: LookupConfig = {
    endpoint: '/api/organization/lookups/currencies',
    mode: 'quick',
    dialogTitleKey: 'LEGAL_ENTITIES.SELECT_CURRENCY',
    columns: [
      { key: 'code', label: 'COMMON.CODE' },
      { key: 'name', label: 'COMMON.NAME' }
    ]
  };

  constructor() {
    this.form = this.buildForm();

    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });

    effect(() => {
      const shouldLockCurrency = this.isCurrencyLocked();
      untracked(() => {
        const currencyControl = this.form.get('functionalCurrencyFk');
        if (!currencyControl) return;
        if (shouldLockCurrency) {
          currencyControl.disable({ emitEvent: false });
        } else {
          currencyControl.enable({ emitEvent: false });
        }
        this.cdr.markForCheck();
      });
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

          if (!this.authService.hasPermission('PERM_LEGAL_ENTITY_UPDATE')) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.loadForEdit(this.entityId()!);
        } else {
          if (!this.authService.hasPermission('PERM_LEGAL_ENTITY_CREATE')) {
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
      legalEntityPk: [{ value: null, disabled: true }],
      legalEntityCode: [{ value: '', disabled: true }],
      legalEntityNameAr: ['', [Validators.required, Validators.maxLength(200)]],
      legalEntityNameEn: ['', [Validators.required, Validators.maxLength(200)]],
      countryFk: [null, Validators.required],
      functionalCurrencyFk: [null, Validators.required],
      taxNumber: [''],
      commercialRegNumber: [''],
      fiscalYearStartMonth: [null, [Validators.min(1), Validators.max(12)]],
      addressLine1: [''],
      addressLine2: [''],
      cityName: [''],
      phone: [''],
      email: ['', Validators.email],
      website: ['', urlValidator],
      statusId: ['ACTIVE', Validators.required]
    });
  }

  loadForCreate(): void {
    const empty = LegalEntityFormMapper.createEmpty();
    this.form.reset(empty);
    this.form.enable();
    this.form.get('legalEntityPk')?.disable();
    this.form.get('legalEntityCode')?.disable();
  }

  loadForEdit(id: number): void {
    this.loading.set(true);
    this.facade.getById(id, (entity) => {
      if (!entity) {
        this.notificationService.error('MESSAGES.RECORD_NOT_FOUND');
        this.navigateBack();
        return;
      }

      const formModel = LegalEntityFormMapper.fromDomain(entity);
      this.form.patchValue(formModel);
      this.form.enable();
      this.form.get('legalEntityPk')?.disable();
      this.form.get('legalEntityCode')?.disable();

      this.facade.getUsageInfo(id);
      this.loading.set(false);
      this.cdr.detectChanges();
    });
  }

  get pageTitleKey(): string {
    return this.isEditMode() ? 'LEGAL_ENTITIES.EDIT' : 'LEGAL_ENTITIES.CREATE';
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const rawValue = this.form.getRawValue();

    if (this.isEditMode() && this.entityId()) {
      const request = LegalEntityFormMapper.toUpdateRequest(rawValue);
      this.facade.update(this.entityId()!, request, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.navigateBack();
      });
    } else {
      const request = LegalEntityFormMapper.toCreateRequest(rawValue);
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
  }

  onCancel(): void { this.navigateBack(); }
  private navigateBack(): void { this.router.navigate(['/organization/legal-entities']); }
}
