import { Component, Input, Output, EventEmitter, inject, ChangeDetectionStrategy, ViewChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';

import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';

import { LookupDetailDto, CreateLookupDetailRequest, UpdateLookupDetailRequest } from '../../models/master-lookup.model';

export interface DetailFormSaveEvent {
  isEdit: boolean;
  createRequest?: CreateLookupDetailRequest;
  updateRequest?: UpdateLookupDetailRequest;
  detailId?: number;
  modalRef: NgbModalRef;
}

/**
 * Standalone component for the Lookup Detail create/edit form modal.
 * Manages its own form state and modal lifecycle.
 * Emits save events to the parent (smart component).
 */
@Component({
  selector: 'app-lookup-detail-form-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ErpFormFieldComponent,
    TranslateModule
  ],
  templateUrl: './lookup-detail-form-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LookupDetailFormModalComponent {
  private fb = inject(FormBuilder);
  private modalService = inject(NgbModal);
  private notificationService = inject(ErpNotificationService);

  @Input() masterLookupId!: number;
  @Input() saving = false;

  @Output() saveDetail = new EventEmitter<DetailFormSaveEvent>();

  @ViewChild('detailModal') detailModalRef!: TemplateRef<unknown>;

  detailForm!: FormGroup;
  editingDetail: LookupDetailDto | null = null;
  private modalInstance: NgbModalRef | null = null;

  constructor() {
    this.initForm();
  }

  private initForm(): void {
    this.detailForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^[A-Z0-9_]+$/)]],
      nameAr: ['', Validators.required],
      nameEn: [''],
      extraValue: [''],
      sortOrder: [0, [Validators.min(0)]]
    });
  }

  open(detail?: LookupDetailDto): void {
    this.editingDetail = detail || null;

    if (detail) {
      this.detailForm.patchValue({
        code: detail.code,
        nameAr: detail.nameAr,
        nameEn: detail.nameEn || '',
        extraValue: detail.extraValue || '',
        sortOrder: detail.sortOrder || 0
      });
      this.detailForm.get('code')?.disable();
    } else {
      this.detailForm.reset({ code: '', nameAr: '', nameEn: '', extraValue: '', sortOrder: 0 });
      this.detailForm.get('code')?.enable();
    }

    this.modalInstance = this.modalService.open(this.detailModalRef, {
      backdrop: 'static',
      keyboard: false,
      centered: true
    });
  }

  onSave(): void {
    if (this.detailForm.invalid) {
      this.detailForm.markAllAsTouched();
      this.notificationService.warning('MESSAGES.FORM_INVALID');
      return;
    }

    const formValue = this.detailForm.getRawValue();

    if (this.editingDetail) {
      const request: UpdateLookupDetailRequest = {
        nameAr: formValue.nameAr,
        nameEn: formValue.nameEn?.trim() || undefined,
        extraValue: formValue.extraValue?.trim() || undefined,
        sortOrder: formValue.sortOrder ?? undefined
      };
      this.saveDetail.emit({
        isEdit: true,
        updateRequest: request,
        detailId: this.editingDetail.id,
        modalRef: this.modalInstance!
      });
    } else {
      const request: CreateLookupDetailRequest = {
        masterLookupId: this.masterLookupId,
        code: formValue.code.toUpperCase(),
        nameAr: formValue.nameAr,
        nameEn: formValue.nameEn?.trim() || undefined,
        extraValue: formValue.extraValue?.trim() || undefined,
        sortOrder: formValue.sortOrder ?? undefined
      };
      this.saveDetail.emit({
        isEdit: false,
        createRequest: request,
        modalRef: this.modalInstance!
      });
    }
  }
}
