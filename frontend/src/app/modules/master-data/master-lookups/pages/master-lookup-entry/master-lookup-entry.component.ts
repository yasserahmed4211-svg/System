import { Component, OnInit, OnDestroy, inject, signal, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, ViewChild, effect, untracked } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
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
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { MasterLookupFacade } from '../../facades/master-lookup.facade';
import { MasterLookupApiService } from '../../services/master-lookup-api.service';
import { LookupDetailsSectionComponent } from '../../components/lookup-details-section/lookup-details-section.component';
import { LookupDetailFormModalComponent, DetailFormSaveEvent } from '../../components/lookup-detail-form-modal/lookup-detail-form-modal.component';
import { LookupDetailDto } from '../../models/master-lookup.model';
import { MasterLookupFormModel, MasterLookupFormMapper } from '../../models/master-lookup-form.model';
import { ConfirmActionDeps, confirmToggleDetailActive, confirmDeleteDetail } from '../../helpers/master-lookup-confirm-actions';

/** Page B (Entry/Form) — Create/Edit master lookup + manage details */
@Component({
  selector: 'app-master-lookup-entry',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpActionBarComponent,
    ErpPermissionDirective,
    LookupDetailsSectionComponent,
    LookupDetailFormModalComponent,
    TranslateModule
  ],
  providers: [MasterLookupFacade, MasterLookupApiService],
  templateUrl: './master-lookup-entry.component.html',
  styleUrl: './master-lookup-entry.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MasterLookupEntryComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private location = inject(Location);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(MasterLookupFacade);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);
  private readonly confirmDeps: ConfirmActionDeps = {
    dialog: inject(ErpDialogService), notify: this.notificationService,
    auth: this.authService, facade: this.facade
  };

  @ViewChild('detailFormModal') detailFormModal!: LookupDetailFormModalComponent;

  /** Signal-based form model — single source of truth for form state */
  readonly model = signal<MasterLookupFormModel>(MasterLookupFormMapper.createEmpty());

  lookupForm!: FormGroup;
  readonly isEditMode = signal(false);
  readonly lookupId = signal<number | null>(null);
  readonly loading = signal(false);

  constructor() {
    this.lookupForm = this.buildForm();

    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });

    effect(() => {
      const detailError = this.facade.detailSaveError();
      if (!detailError) return;
      untracked(() => this.notificationService.error(detailError));
    });
  }

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idParam = params.get('id');
        if (idParam) {
          this.isEditMode.set(true);
          this.lookupId.set(Number(idParam));

          if (!this.authService.hasPermission('PERM_MASTER_LOOKUP_UPDATE')) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.loadForEdit(this.lookupId()!);
        } else {
          if (!this.authService.hasPermission('PERM_MASTER_LOOKUP_CREATE')) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.isEditMode.set(false);
          this.lookupId.set(null);
          this.loadForCreate();
        }
        this.cdr.detectChanges();
      });
  }

  ngOnDestroy(): void { this.facade.clearCurrentMasterLookup(); }

  // ============================================
  // FORM MODEL — unified create / edit lifecycle
  // ============================================

  private buildForm(): FormGroup {
    return this.fb.group({
      lookupKey: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/)]],
      lookupName: ['', Validators.required],
      lookupNameEn: [''],
      description: ['']
    });
  }

  /** Initialise form for Create mode */
  loadForCreate(): void {
    const empty = MasterLookupFormMapper.createEmpty();
    this.model.set(empty);
    this.lookupForm.reset(empty);
    this.lookupForm.enable();
  }

  /** Load existing entity and initialise form for Edit mode */
  loadForEdit(id: number): void {
    this.loading.set(true);
    this.facade.getMasterLookupById(id, (lookup) => {
      if (!lookup) { this.notificationService.error('MESSAGES.RECORD_NOT_FOUND'); this.navigateBack(); return; }

      const formModel = MasterLookupFormMapper.fromDomain(lookup);
      this.model.set(formModel);
      this.lookupForm.patchValue(formModel);
      this.lookupForm.enable();
      this.lookupForm.get('lookupKey')?.disable();

      this.facade.loadLookupDetails(id);
      this.facade.getUsageInfo(id);
      this.loading.set(false);
      this.cdr.detectChanges();
    });
  }

  /** Unified save: delegates to create or update via the mapper */
  save(): void {
    if (this.lookupForm.invalid) {
      this.lookupForm.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const currentModel: MasterLookupFormModel = this.lookupForm.getRawValue();

    if (this.isEditMode() && this.lookupId()) {
      const request = MasterLookupFormMapper.toUpdateRequest(currentModel);
      this.facade.updateMasterLookup(this.lookupId()!, request, () =>
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS'));
    } else {
      const request = MasterLookupFormMapper.toCreateRequest(currentModel);
      this.facade.createMasterLookup(request, (c) => {
        this.notificationService.success('MESSAGES.CREATE_SUCCESS');
        // Switch to edit mode in-place — avoids component destroy/recreate + loading flicker
        this.isEditMode.set(true);
        this.lookupId.set(c.id);
        this.lookupForm.get('lookupKey')?.disable();
        this.location.replaceState(`/master-data/master-lookups/edit/${c.id}`);
        this.facade.loadLookupDetails(c.id, false);
        this.facade.getUsageInfo(c.id);
        this.cdr.detectChanges();
      });
    }
  }

  onCancel(): void { this.navigateBack(); }
  private navigateBack(): void { this.router.navigate(['/master-data/master-lookups']); }

  /** Force lookupKey value to uppercase so the pattern validator works correctly */
  onLookupKeyInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (input.value !== upper) {
      this.lookupForm.get('lookupKey')?.setValue(upper, { emitEvent: true });
    }
  }

  openDetailModal(detail?: LookupDetailDto): void {
    if (!this.lookupId()) {
      this.notificationService.warning('MASTER_LOOKUPS.SAVE_BEFORE_ADDING_DETAILS');
      return;
    }
    if (detail && !this.authService.hasPermission('PERM_MASTER_LOOKUP_UPDATE')) {
      this.notificationService.warning('MESSAGES.NO_PERMISSION'); return;
    }
    this.detailFormModal.open(detail);
  }

  onDetailSave(event: DetailFormSaveEvent): void {
    if (event.isEdit && event.updateRequest && event.detailId != null) {
      this.facade.updateLookupDetail(event.detailId, this.lookupId()!, event.updateRequest, () => {
        this.notificationService.success('MESSAGES.UPDATE_SUCCESS'); event.modalRef.close();
      });
    } else if (event.createRequest) {
      this.facade.createLookupDetail(event.createRequest, () => {
        this.notificationService.success('MESSAGES.CREATE_SUCCESS'); event.modalRef.close();
      });
    }
  }

  toggleDetailActive(d: LookupDetailDto): void { confirmToggleDetailActive(this.confirmDeps, d, this.lookupId()!); }
  deleteDetail(d: LookupDetailDto): void { confirmDeleteDetail(this.confirmDeps, d, this.lookupId()!); }
  sortDetails(field: string): void { if (this.lookupId()) this.facade.toggleDetailSort(this.lookupId()!, field); }
  get pageTitleKey(): string { return this.isEditMode() ? 'MASTER_LOOKUPS.EDIT' : 'MASTER_LOOKUPS.CREATE'; }
}
