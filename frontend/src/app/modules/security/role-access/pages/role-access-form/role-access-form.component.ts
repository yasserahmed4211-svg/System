import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  OnDestroy,
  effect,
  inject,
  untracked
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';
import { ErpDualListComponent, DualListItem } from 'src/app/shared/components/erp-dual-list/erp-dual-list.component';

import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';

import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';

import { RoleAccessFacade } from '../../facades/role-access.facade';
import { RoleAccessApiService } from '../../services/role-access-api.service';
import { ActivePageDto, AddRolePagesRequestItem, RoleDto, RolePagePermissionDto } from '../../models/role-access.model';
import { confirmRemoveRolePage, RoleConfirmActionDeps } from '../../helpers/role-confirm-actions';

import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-role-access-form',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    TranslateModule,
    ErpPermissionDirective,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpActionBarComponent,
    ErpDualListComponent
  ],
  templateUrl: './role-access-form.component.html',
  styleUrl: './role-access-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [RoleAccessFacade, RoleAccessApiService]
})
export class RoleAccessFormComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly modalService = inject(NgbModal);

  readonly translate = inject(TranslateService);
  readonly facade = inject(RoleAccessFacade);

  private readonly dialogService = inject(ErpDialogService);
  private readonly notificationService = inject(ErpNotificationService);

  roleForm!: FormGroup;
  isEditMode = false;
  roleId: number | null = null;

  permissions: RolePagePermissionDto[] = [];

  addPagesModalRef: NgbModalRef | null = null;
  copyFromModalRef: NgbModalRef | null = null;

  dualListAvailableItems: DualListItem[] = [];
  dualListSelectedItems: DualListItem[] = [];

  availableRolesToCopy: RoleDto[] = [];
  selectedSourceRoleId: number | null = null;

  get role(): RoleDto | null {
    return this.facade.selectedRole();
  }

  get activePages(): ActivePageDto[] {
    return this.facade.activePages();
  }

  get isLoading(): boolean {
    return this.facade.loading();
  }

  get isSaving(): boolean {
    return this.facade.saving();
  }

  constructor() {
    this.initForm();

    effect(() => {
      const saveError = this.facade.saveError();
      if (!saveError) return;
      untracked(() => this.notificationService.error(saveError));
    });

    effect(() => {
      const pages = this.facade.rolePages();
      untracked(() => {
        this.permissions = (pages ?? []).map((p) => ({ ...p }));
        this.cdr.markForCheck();
      });
    });

    effect(() => {
      const role = this.facade.selectedRole();
      if (!role) return;
      untracked(() => {
        this.roleForm.patchValue(
          {
            name: role.roleName,
            active: role.active
          },
          { emitEvent: false }
        );
        if (this.isEditMode) this.roleForm.disable({ emitEvent: false });
        this.cdr.markForCheck();
      });
    });

    effect(() => {
      if (!this.roleId) return;
      const roles = this.facade.roles();
      untracked(() => {
        this.availableRolesToCopy = roles.filter((r) => r.id !== this.roleId);
      });
    });
  }

  ngOnInit(): void {
    this.facade.loadActivePages();

    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const roleIdParam = params.get('roleId');

      if (roleIdParam) {
        this.isEditMode = true;
        this.roleId = Number(roleIdParam);
        this.roleForm.disable();

        this.facade.loadRoleDetails(this.roleId);
        this.facade.loadRolePages(this.roleId);

        // Preload roles list for Copy From modal
        this.facade.setFilters([]);
        this.facade.setSize(50);
        this.facade.setPage(0);
        this.facade.loadRoles();
      } else {
        this.isEditMode = false;
        this.roleId = null;
        this.roleForm.enable();
      }

      this.cdr.detectChanges();
    });
  }

  private initForm(): void {
    this.roleForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      active: [true]
    });
  }

  private deriveRoleCode(roleName: string): string {
    return String(roleName ?? '')
      .trim()
      .toUpperCase()
      .replace(/\s+/g, '_')
      .replace(/[^A-Z0-9_]/g, '');
  }

  navigateBack(): void {
    this.router.navigate(['/security/role-access']);
  }

  onCreateRole(): void {
    if (this.roleForm.invalid) {
      this.roleForm.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const value = this.roleForm.getRawValue() as { name: string; active: boolean };
    this.facade.createRole(
      {
        roleName: value.name,
        roleCode: this.deriveRoleCode(value.name),
        active: value.active
      },
      (created) => {
        this.notificationService.success('MESSAGES.CREATE_SUCCESS');
        this.router.navigate(['/security/role-access/edit', created.id]);
      }
    );
  }

  onToggleAllForRow(row: RolePagePermissionDto, checked: boolean): void {
    row.create = checked;
    row.update = checked;
    row.delete = checked;
  }

  isAllChecked(row: RolePagePermissionDto): boolean {
    return !!row.create && !!row.update && !!row.delete;
  }

  onSavePermissions(): void {
    if (!this.roleId) return;

    this.facade.syncRolePages(this.roleId, this.permissions, () => {
      this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
    });
  }

  confirmRemovePage(pageCode: string): void {
    if (!this.roleId) return;

    const deps: RoleConfirmActionDeps = {
      dialog: this.dialogService,
      notify: this.notificationService,
      facade: this.facade
    };
    confirmRemoveRolePage(deps, this.roleId, pageCode, () => {});
  }

  openAddPagesModal(content: any): void {
    if (!this.roleId) return;

    const assigned = new Set(this.permissions.map((p) => p.pageCode));
    const available = this.activePages
      .filter((p) => p.active && !assigned.has(p.pageCode))
      .map((p) => ({
        id: p.pageCode,
        label: this.translate.currentLang === 'ar' ? (p.nameAr || p.pageCode) : (p.nameEn || p.pageCode),
        secondaryLabel: p.pageCode
      }));

    this.dualListAvailableItems = available;
    this.dualListSelectedItems = [];

    this.addPagesModalRef = this.modalService.open(content, { size: 'lg', centered: true });
  }

  onPagesSelectionChanged(selectedItems: DualListItem[]): void {
    this.dualListSelectedItems = selectedItems;
  }

  addSelectedPages(defaultCrud: { create: boolean; update: boolean; delete: boolean }): void {
    if (!this.roleId) return;

    const items: AddRolePagesRequestItem[] = this.dualListSelectedItems.map((i) => ({
      pageCode: String(i.id),
      create: defaultCrud.create,
      update: defaultCrud.update,
      delete: defaultCrud.delete
    }));

    if (items.length === 0) {
      this.notificationService.warning('COMMON.NO_DATA');
      return;
    }

    this.facade.addRolePages(this.roleId, items, () => {
      this.notificationService.success('MESSAGES.CREATE_SUCCESS');
      this.addPagesModalRef?.close();
      this.addPagesModalRef = null;
    });
  }

  openCopyFromModal(content: any): void {
    if (!this.roleId) return;
    this.selectedSourceRoleId = null;

    this.copyFromModalRef = this.modalService.open(content, { size: 'md', centered: true });
  }

  onCopyFromConfirm(): void {
    if (!this.roleId || !this.selectedSourceRoleId) return;

    this.facade.copyFromRole(this.roleId, this.selectedSourceRoleId, () => {
      this.notificationService.success('MESSAGES.UPDATE_SUCCESS');
      this.copyFromModalRef?.close();
      this.copyFromModalRef = null;
    });
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
