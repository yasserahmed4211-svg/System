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
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';
import { ErpLookupFieldComponent } from 'src/app/shared/components/erp-lookup-field/erp-lookup-field.component';
import { LookupConfig } from 'src/app/core/lookup/lookup.model';

import { RegionFacade } from '../../facades/region.facade';
import { RegionApiService } from '../../services/region-api.service';
import { RegionFormMapper } from '../../models/region-form.model';
import { LookupService } from 'src/app/core/services/lookup.service';
import { LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

@Component({
  selector: 'app-region-entry',
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
  providers: [RegionFacade, RegionApiService],
  templateUrl: './region-entry.component.html',
  styleUrl: './region-entry.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegionEntryComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(RegionFacade);
  private readonly lookupService = inject(LookupService);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  readonly isEditMode = signal(false);
  readonly regionId = signal<number | null>(null);
  readonly loading = signal(false);

  form!: FormGroup;
  statusOptions: LookupSelectOption[] = [];

  readonly legalEntityLookupConfig: LookupConfig = {
    endpoint: '/api/organization/lookups/legal-entities',
    mode: 'quick',
    dialogTitleKey: 'REGIONS.SELECT_LEGAL_ENTITY',
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
          this.regionId.set(Number(idParam));

          if (!this.authService.hasPermission('PERM_REGION_UPDATE')) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.loadForEdit(this.regionId()!);
        } else {
          if (!this.authService.hasPermission('PERM_REGION_CREATE')) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.isEditMode.set(false);
          this.regionId.set(null);
          this.loadForCreate();
        }
        this.cdr.detectChanges();
      });
  }

  ngOnDestroy(): void { this.facade.clearCurrentEntity(); }

  private buildForm(): FormGroup {
    return this.fb.group({
      regionPk: [{ value: null, disabled: true }],
      regionCode: [{ value: '', disabled: true }],
      legalEntityFk: [null, Validators.required],
      regionNameAr: ['', [Validators.required, Validators.maxLength(200)]],
      regionNameEn: ['', [Validators.required, Validators.maxLength(200)]],
      descriptionAr: [''],
      statusId: ['ACTIVE', Validators.required]
    });
  }

  loadForCreate(): void {
    const empty = RegionFormMapper.createEmpty();
    this.form.reset(empty);
    this.form.enable();
    this.form.get('regionPk')?.disable();
    this.form.get('regionCode')?.disable();
  }

  loadForEdit(id: number): void {
    this.loading.set(true);
    this.facade.getById(id, (region) => {
      if (!region) {
        this.notificationService.error('MESSAGES.RECORD_NOT_FOUND');
        this.navigateBack();
        return;
      }

      const formModel = RegionFormMapper.fromDomain(region);
      this.form.patchValue(formModel);
      this.form.enable();
      // Immutable fields in edit mode
      this.form.get('regionPk')?.disable();
      this.form.get('regionCode')?.disable();
      this.form.get('legalEntityFk')?.disable();

      this.facade.getUsageInfo(id);
      this.loading.set(false);
      this.cdr.detectChanges();
    });
  }

  get pageTitleKey(): string {
    return this.isEditMode() ? 'REGIONS.EDIT' : 'REGIONS.CREATE';
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const rawValue = this.form.getRawValue();

    if (this.isEditMode() && this.regionId()) {
      const request = RegionFormMapper.toUpdateRequest(rawValue);
      this.facade.update(this.regionId()!, request, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
        this.navigateBack();
      });
    } else {
      const request = RegionFormMapper.toCreateRequest(rawValue);
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
  private navigateBack(): void { this.router.navigate(['/organization/regions']); }
}
