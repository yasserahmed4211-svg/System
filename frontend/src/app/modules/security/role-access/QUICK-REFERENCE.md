# Blueprint Level 2 - Quick Reference for Role Access

## 🎯 What is Blueprint Level 2?

**Page-Based CRUD pattern** - The DEFAULT for admin and master data screens.

## 📐 Structure

```
Page A: List/Search     →     Page B: Create/Edit Form
(role-access-control)         (role-access-form)
```

---

## ✅ Quick Compliance Checklist

### When implementing ANY admin screen:

**Page A (List):**
- [ ] Uses `ErpListComponent` base class
- [ ] Has `SpecificationFilterComponent` for filters
- [ ] Uses ag-Grid for results table
- [ ] Add button → **navigates** to create page (not modal!)
- [ ] Edit action → **navigates** to edit page
- [ ] Delete action → inline with `ErpDialogService.confirm()`
- [ ] Empty state → `ErpEmptyStateComponent`
- [ ] **NO** forms on this page
- [ ] **NO** create/edit logic on this page

**Page B (Form):**
- [ ] Separate route: `/feature/create` and `/feature/edit/:id`
- [ ] Uses `ErpFormFieldComponent` for all inputs
- [ ] Uses `ErpSectionComponent` for grouping
- [ ] Has `ErpBackButtonComponent` for navigation
- [ ] Save validates form before submit
- [ ] Save shows success toast (`ErpNotificationService`)
- [ ] **NO** search/filter on this page
- [ ] **NO** data tables on this page (except read-only matrix)

**Universal:**
- [ ] Zero hardcoded text (all translation keys)
- [ ] All actions have `[erpPermission]="..."` directive
- [ ] Component → Facade → Service (no direct API calls)
- [ ] All operations provide user feedback

---

## 🚫 Common Mistakes to Avoid

| ❌ DON'T | ✅ DO |
|----------|-------|
| Put create/edit form in modal | Navigate to separate page |
| Call API service from component | Inject facade, call facade method |
| Hardcode "Save", "Delete" text | Use `'COMMON.SAVE' \| translate` |
| Silent save without notification | Always show success/error feedback |
| Mix search and form on same page | Separate page for each concern |
| Create local form components | Use `ErpFormFieldComponent` |
| Skip permission checks | Add `erpPermission` on all actions |
| Forget confirmation on delete | Always use `ErpDialogService.confirm()` |

---

## 📚 Shared Components Reference

**Must-use components:**

```typescript
// List page
import { ErpListComponent } from '@shared/base/erp-list.component';
import { SpecificationFilterComponent } from '@shared/components/specification-filter';
import { ErpEmptyStateComponent } from '@shared/components/erp-empty-state';

// Form page
import { ErpBackButtonComponent } from '@shared/components/erp-back-button';
import { ErpFormFieldComponent } from '@shared/components/erp-form-field';
import { ErpSectionComponent } from '@shared/components/erp-section';
```

**Must-use services:**

```typescript
import { ErpDialogService } from '@shared/services/erp-dialog.service';
import { ErpNotificationService } from '@shared/services/erp-notification.service';
```

**Must-use directives:**

```typescript
import { ErpPermissionDirective } from '@shared/directives/erp-permission.directive';
```

---

## 🎨 Code Templates

### **List Page Component Structure:**

```typescript
@Component({
  selector: 'app-feature-list',
  standalone: true,
  imports: [
    CommonModule, SharedModule, AgGridAngular, TranslateModule,
    SpecificationFilterComponent, ErpEmptyStateComponent, ErpPermissionDirective
  ],
  providers: [FeatureFacade]
})
export class FeatureListComponent extends ErpListComponent implements OnInit {
  readonly facade = inject(FeatureFacade);
  private readonly router = inject(Router);
  private readonly dialogService = inject(ErpDialogService);
  private readonly notificationService = inject(ErpNotificationService);

  get rowData() { return this.facade.items(); }
  get isLoading() { return this.facade.loading(); }

  navigateToCreate(): void {
    this.router.navigate(['/module/feature/create']);
  }

  onEdit(id: number): void {
    this.router.navigate(['/module/feature/edit', id]);
  }

  onDelete(item: ItemDto): void {
    this.dialogService.confirm({
      title: 'FEATURE.CONFIRM_DELETE',
      message: 'FEATURE.CONFIRM_DELETE_MESSAGE'
    }).then((confirmed) => {
      if (confirmed) this.facade.deleteItem(item.id);
    });
  }
}
```

### **Form Page Component Structure:**

```typescript
@Component({
  selector: 'app-feature-form',
  standalone: true,
  imports: [
    CommonModule, SharedModule, ReactiveFormsModule, TranslateModule,
    ErpBackButtonComponent, ErpFormFieldComponent, ErpSectionComponent, ErpPermissionDirective
  ],
  providers: [FeatureFacade]
})
export class FeatureFormComponent implements OnInit {
  readonly facade = inject(FeatureFacade);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notificationService = inject(ErpNotificationService);

  form!: FormGroup;
  isEditMode = false;
  itemId: number | null = null;

  ngOnInit(): void {
    this.initForm();
    this.itemId = Number(this.route.snapshot.paramMap.get('id'));
    this.isEditMode = !!this.itemId;
    
    if (this.isEditMode) {
      this.facade.loadItem(this.itemId);
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      active: [true]
    });
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const data = this.form.value;
    if (this.isEditMode) {
      this.facade.updateItem(this.itemId!, data);
    } else {
      this.facade.createItem(data);
    }

    // Success notification via facade effect
    effect(() => {
      const saving = this.facade.saving();
      const error = this.facade.saveError();
      if (!saving && !error) {
        this.notificationService.success('FEATURE.SAVE_SUCCESS');
        this.navigateBack();
      }
    });
  }

  navigateBack(): void {
    this.router.navigate(['/module/feature']);
  }
}
```

### **Template Structure (List Page):**

```html
<app-card [cardTitle]="'FEATURE.TITLE' | translate">
  <div class="card-header-right">
    <button class="btn btn-success" 
            [erpPermission]="'FEATURE.CREATE'"
            (click)="navigateToCreate()">
      <i class="ti ti-plus"></i>
      {{ 'FEATURE.ADD' | translate }}
    </button>
  </div>

  @if (showFilters) {
    <erp-specification-filter
      [availableFields]="fields"
      (apply)="onFiltersApply($event)" />
  }

  <div class="card-body">
    @if (!isLoading && hasError) {
      <erp-empty-state
        icon="ti ti-alert-circle"
        titleKey="ERRORS.OPERATION_FAILED"
        (actionClicked)="refreshData()" />
    } @else if (!isLoading && rowData.length === 0) {
      <erp-empty-state
        icon="ti ti-database"
        titleKey="COMMON.NO_DATA" />
    } @else {
      <ag-grid-angular
        [rowData]="rowData"
        [columnDefs]="columnDefs" />
    }
  </div>
</app-card>
```

### **Template Structure (Form Page):**

```html
<app-card [cardTitle]="'FEATURE.TITLE' | translate">
  <div class="card-header-right">
    <erp-back-button (backClicked)="navigateBack()" />
  </div>

  <div class="card-body">
    <erp-section titleKey="FEATURE.INFO">
      <form [formGroup]="form" class="row">
        <div class="col-md-6">
          <erp-form-field 
            labelKey="FEATURE.NAME" 
            [control]="form.get('name')" 
            [required]="true">
            <input type="text" 
                   class="form-control" 
                   formControlName="name" />
          </erp-form-field>
        </div>
      </form>

      <div class="mt-3">
        <button class="btn btn-success"
                [erpPermission]="'FEATURE.UPDATE'"
                [disabled]="form.invalid"
                (click)="onSave()">
          <i class="ti ti-device-floppy"></i>
          {{ 'COMMON.SAVE' | translate }}
        </button>
      </div>
    </erp-section>
  </div>
</app-card>
```

---

## 🔍 Quick Validation

Before submitting code, verify:

1. ✅ Run: `npm run build` → Must pass
2. ✅ Search codebase for hardcoded text → Must find none
3. ✅ Check all buttons have `erpPermission` directive
4. ✅ Verify facade boundary: Component → Facade → Service
5. ✅ Test save → Must show success notification
6. ✅ Test delete → Must show confirmation dialog

---

## 📖 Full Documentation

- **Complete File Tuning:** [FILE-TUNING-BLUEPRINT-LEVEL-2.md](FILE-TUNING-BLUEPRINT-LEVEL-2.md)
- **Compliance Report:** [BLUEPRINT-COMPLIANCE-SUMMARY.md](BLUEPRINT-COMPLIANCE-SUMMARY.md)
- **Architecture Rules:** `ai-governance/frontend-guidelines/frontend.rules.md` (Section 24)

---

**Role Access is your reference implementation for Blueprint Level 2! 🎯**
