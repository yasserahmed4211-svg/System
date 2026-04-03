/**
 * ═══════════════════════════════════════════════════════════════════
 *  LOOKUP FIELD — USAGE EXAMPLE
 * ═══════════════════════════════════════════════════════════════════
 *
 *  This file demonstrates how feature components consume the unified
 *  Lookup Selection Framework.  It is NOT wired into routing — it
 *  serves as a copy-paste reference for developers.
 *
 *  Key rules:
 *   • Feature modules NEVER call HTTP for lookups
 *   • Feature modules NEVER implement custom dropdowns
 *   • All entity selection uses <erp-lookup-field>
 *   • The form stores ONLY the selected ID
 * ═══════════════════════════════════════════════════════════════════
 */

import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { LookupConfig } from 'src/app/core/lookup/lookup.model';
import { ErpFormFieldComponent } from 'src/app/shared/components/erp-form-field/erp-form-field.component';
import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpFormActionsComponent } from 'src/app/shared/components/erp-form-actions/erp-form-actions.component';
import { ErpLookupFieldComponent } from 'src/app/shared/components/erp-lookup-field/erp-lookup-field.component';

/**
 * Example: Invoice Form using Lookup Fields
 *
 * Demonstrates:
 *  1. Quick mode (autocomplete) for customer selection
 *  2. Advanced mode (dialog) for GL account selection
 */
@Component({
  selector: 'app-invoice-form-example',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslateModule,
    ErpFormFieldComponent,
    ErpSectionComponent,
    ErpFormActionsComponent,
    ErpLookupFieldComponent
  ],
  template: `
    <erp-section titleKey="INVOICE.BASIC_INFO">
      <form [formGroup]="invoiceForm">

        <!-- ① Quick Lookup: Customer (autocomplete) -->
        <erp-form-field
          labelKey="INVOICE.CUSTOMER"
          [control]="invoiceForm.get('customerIdFk')"
          [required]="true"
        >
          <erp-lookup-field
            formControlName="customerIdFk"
            [config]="customerLookupConfig"
          ></erp-lookup-field>
        </erp-form-field>

        <!-- ② Advanced Lookup: GL Account (dialog with dynamic columns) -->
        <erp-form-field
          labelKey="INVOICE.GL_ACCOUNT"
          [control]="invoiceForm.get('glAccountIdFk')"
          [required]="true"
        >
          <erp-lookup-field
            formControlName="glAccountIdFk"
            [config]="glAccountLookupConfig"
          ></erp-lookup-field>
        </erp-form-field>

        <!-- ③ Regular field (for contrast) -->
        <erp-form-field
          labelKey="INVOICE.AMOUNT"
          [control]="invoiceForm.get('amount')"
          [required]="true"
        >
          <input
            type="number"
            class="form-control"
            formControlName="amount"
          />
        </erp-form-field>

      </form>
    </erp-section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InvoiceFormExampleComponent {
  private readonly fb = inject(FormBuilder);

  // ── Form ─────────────────────────────────────────────────────────

  readonly invoiceForm = this.fb.group({
    /** Stores ONLY the customer ID (number | null) */
    customerIdFk: [null as number | null, [Validators.required]],
    /** Stores ONLY the GL account ID (number | null) */
    glAccountIdFk: [null as number | null, [Validators.required]],
    amount: [null as number | null, [Validators.required, Validators.min(0)]]
  });

  // ── Lookup Configs ───────────────────────────────────────────────

  /**
   * Quick mode — inline autocomplete for customer search.
   * No columns needed since results show as a flat list.
   */
  readonly customerLookupConfig: LookupConfig = {
    endpoint: '/api/customers/lookup',
    mode: 'quick',
    minChars: 2,
    pageSize: 10,
    placeholderKey: 'INVOICE.SEARCH_CUSTOMER'
  };

  /**
   * Advanced mode — dialog with dynamic columns for GL account selection.
   * Columns are fully configurable; the dialog renders them dynamically.
   */
  readonly glAccountLookupConfig: LookupConfig = {
    endpoint: '/api/gl/accounts/lookup',
    mode: 'advanced',
    pageSize: 15,
    dialogTitleKey: 'GL.SELECT_ACCOUNT',
    placeholderKey: 'GL.SEARCH_ACCOUNT',
    columns: [
      { key: 'code', label: 'GL.ACCOUNT_CODE', width: '140px' },
      { key: 'name', label: 'GL.ACCOUNT_NAME' },
      { key: 'type', label: 'GL.ACCOUNT_TYPE', width: '160px' },
      { key: 'balance', label: 'GL.BALANCE', width: '140px' }
    ]
  };

  // ── Actions ──────────────────────────────────────────────────────

  onSave(): void {
    if (this.invoiceForm.invalid) {
      this.invoiceForm.markAllAsTouched();
      return;
    }

    const payload = this.invoiceForm.getRawValue();
    // payload.customerIdFk  → number (selected ID only)
    // payload.glAccountIdFk → number (selected ID only)
    // payload.amount        → number
    console.log('Invoice payload:', payload);
  }
}
