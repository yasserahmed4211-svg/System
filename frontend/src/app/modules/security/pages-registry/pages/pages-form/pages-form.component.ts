import { Component, OnInit, OnDestroy, inject, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';

import { PagesFacade } from 'src/app/modules/security/pages-registry/facades/pages.facade';
import { PagesApiService } from 'src/app/modules/security/pages-registry/services/pages-api.service';
import { PageDto, CreatePageRequest, UpdatePageRequest } from 'src/app/modules/security/pages-registry/models/page.model';

import { TranslateModule, TranslateService } from '@ngx-translate/core';

/**
 * PagesFormComponent (Page B of Unified Blueprint)
 * 
 * Purpose: Creating or editing a single record
 * 
 * Contains:
 * - Form-only interface
 * - Save / Cancel actions
 * 
 * Prohibitions:
 * - NO tables
 * - NO search functionality
 * - NO modals
 * 
 * Behavior:
 * - Save → Persist → Notify → Navigate back to Search Page
 * - Cancel → Navigate back to Search Page
 * - Mode determined by route (create / edit/:id)
 * 
 * @requirement FE-REQ-PAGES-001
 * @task TASK-FE-PAGES-001
 * @blueprint Unified Blueprint - Page B (Form)
 */
@Component({
  selector: 'app-pages-form',
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
  templateUrl: './pages-form.component.html',
  styleUrl: './pages-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PagesFacade, PagesApiService]
})
export class PagesFormComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(PagesFacade);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly authService = inject(AuthenticationService);

  private readonly PERM_CREATE = 'PERM_PAGE_CREATE';
  private readonly PERM_UPDATE = 'PERM_PAGE_UPDATE';

  // Form state
  pageForm!: FormGroup;
  isEditMode = false; // true = editing existing, false = creating new
  pageId: number | null = null;
  loading = false;

  // Dropdown data
  get modules(): string[] { return this.facade.modules(); }
  get activePages(): PageDto[] { return this.facade.activePages(); }

  constructor() {
    this.initForm();

    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });
  }

  ngOnInit(): void {
    // Load dropdown data
    this.facade.loadModules();
    this.facade.loadActivePages();

    // Determine mode from route
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idParam = params.get('id');
        if (idParam) {
          this.isEditMode = true;
          this.pageId = Number(idParam);

          if (!this.authService.hasPermission(this.PERM_UPDATE)) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.loadPageForEdit(this.pageId);
        } else {
          if (!this.authService.hasPermission(this.PERM_CREATE)) {
            this.notificationService.error('MESSAGES.NO_PERMISSION');
            this.navigateBack();
            return;
          }

          this.isEditMode = false;
          this.pageId = null;
          this.setupCreateMode();
        }
        this.cdr.detectChanges();
      });
  }

  private initForm(): void {
    this.pageForm = this.fb.group({
      pageCode: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]*$/)]],
      nameAr: ['', Validators.required],
      nameEn: ['', Validators.required],
      route: ['', [Validators.required, Validators.pattern(/^\/[a-z0-9\-\/]*$/)]],
      icon: [''],
      module: [''],
      parentId: [null],
      displayOrder: [0, [Validators.required, Validators.min(0)]],
      active: [true]
    });
  }

  private setupCreateMode(): void {
    this.pageForm.reset({
      pageCode: '',
      nameAr: '',
      nameEn: '',
      route: '',
      icon: '',
      module: '',
      parentId: null,
      displayOrder: 0,
      active: true
    });
    this.pageForm.enable();
  }

  private loadPageForEdit(id: number): void {
    this.loading = true;
    this.facade.getPageById(id, (page) => {
      if (page) {
        this.pageForm.patchValue({
          pageCode: page.pageCode,
          nameAr: page.nameAr,
          nameEn: page.nameEn,
          route: page.route,
          icon: page.icon || '',
          module: page.module || '',
          parentId: page.parentId ?? null,
          displayOrder: page.displayOrder,
          active: page.active
        });
        
        // Disable pageCode in edit mode (cannot be changed)
        this.pageForm.enable();
        this.pageForm.get('pageCode')?.disable();
      } else {
        this.notificationService.error('MESSAGES.PAGE_NOT_FOUND');
        this.navigateBack();
      }
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  // ========================================
  // FORM ACTIONS (Blueprint Page B)
  // ========================================

  onSave(): void {
    if (this.pageForm.invalid) {
      this.pageForm.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    type PageFormRawValue = {
      pageCode: string;
      nameAr: string;
      nameEn: string;
      route: string;
      icon: string;
      module: string;
      parentId: number | null;
      displayOrder: number;
      active: boolean;
    };

    const formValue = this.pageForm.getRawValue() as PageFormRawValue; // getRawValue includes disabled fields

    if (this.isEditMode && this.pageId) {
      this.updatePage(formValue);
    } else {
      this.createPage(formValue);
    }
  }

  private createPage(formValue: { pageCode: string; nameAr: string; nameEn: string; route: string; icon: string; module: string; parentId: number | null; displayOrder: number; active: boolean; }): void {
    const createRequest: CreatePageRequest = {
      pageCode: formValue.pageCode,
      nameAr: formValue.nameAr,
      nameEn: formValue.nameEn,
      route: formValue.route,
      icon: formValue.icon?.trim() ? formValue.icon.trim() : undefined,
      module: formValue.module?.trim() ? formValue.module.trim() : undefined,
      parentId: formValue.parentId ?? undefined,
      displayOrder: formValue.displayOrder,
      active: formValue.active
    };
    
    this.facade.createPage(createRequest, () => {
      this.notificationService.success('MESSAGES.CREATE_SUCCESS');
      this.navigateBack();
    });
  }

  private updatePage(formValue: { nameAr: string; nameEn: string; route: string; icon: string; module: string; parentId: number | null; displayOrder: number; active: boolean; }): void {
    if (!this.pageId) return;

    const updateRequest: UpdatePageRequest = {
      nameAr: formValue.nameAr,
      nameEn: formValue.nameEn,
      route: formValue.route,
      icon: formValue.icon?.trim() ? formValue.icon.trim() : undefined,
      module: formValue.module?.trim() ? formValue.module.trim() : undefined,
      parentId: formValue.parentId ?? undefined,
      displayOrder: formValue.displayOrder,
      active: formValue.active
    };
    
    this.facade.updatePage(this.pageId, updateRequest, () => {
      this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
      this.navigateBack();
    });
  }

  onCancel(): void {
    this.navigateBack();
  }

  private navigateBack(): void {
    this.router.navigate(['/security/pages-registry']);
  }

  // ========================================
  // FORM HELPERS
  // ========================================
  get pageTitleKey(): string {
    return this.isEditMode ? 'PAGES.EDIT_PAGE' : 'PAGES.CREATE_PAGE';
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
