import { Component, OnInit, OnDestroy, inject, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, HostListener, effect, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { ErpDialogService } from 'src/app/shared/services/erp-dialog.service';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { SpecificationFilterComponent } from 'src/app/shared/components/specification-filter/specification-filter.component';
import { SpecFieldOption, SpecFilter, SpecOperatorOption } from 'src/app/shared/models';

import { GlFacade } from 'src/app/modules/finance/gl/facades/gl.facade';
import { GlApiService } from 'src/app/modules/finance/gl/services/gl-api.service';
import {
  AccountChartTreeNode,
  AccountChartDto,
  GL_LOOKUP_KEYS,
  CreateAccountRequest,
  UpdateAccountRequest
} from 'src/app/modules/finance/gl/models/gl.model';

import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LookupService } from 'src/app/core/services/lookup.service';
import { LookupDetail, LookupSelectOption } from 'src/app/core/models/lookup-detail.model';

// ── Inline editing state interfaces ──────────────────────────

export interface InlineEditState {
  pk: number;
  accountChartName: string;
  accountType: string;
  isActive: boolean;
  dirty: boolean;
}

export interface InlineAddState {
  parentPk: number | null;
  organizationFk: number;
  accountChartName: string;
  accountType: string;
  isActive: boolean;
}

/**
 * AccountsTreeComponent – Full CRUD management console for Chart of Accounts.
 *
 * Features:
 * - Hierarchical tree with expand/collapse
 * - Inline edit (expand panel below node)
 * - Inline add root / add child
 * - Inline delete with validation
 * - Quick filter by name
 * - Keyboard shortcuts (Escape to cancel)
 *
 * @requirement FE-REQ-GL-001 §3.5
 * @task TASK-FE-GL-001
 */
@Component({
  selector: 'app-accounts-tree',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    FormsModule,
    ErpEmptyStateComponent,
    ErpPermissionDirective,
    TranslateModule,
    SpecificationFilterComponent
  ],
  templateUrl: './accounts-tree.component.html',
  styleUrl: './accounts-tree.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [GlFacade, GlApiService]
})
export class AccountsTreeComponent implements OnInit, OnDestroy {
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  private dialog = inject(ErpDialogService);
  private notification = inject(ErpNotificationService);
  readonly translate = inject(TranslateService);
  readonly facade = inject(GlFacade);
  private readonly lookupService = inject(LookupService);

  /** Watch facade signals and trigger OnPush change detection */
  private readonly signalWatcher = effect(() => {
    this.facade.accountTree();
    this.facade.accountTreeLoading();
    this.facade.saving();
    untracked(() => this.cdr.markForCheck());
  });

  /** Dynamic account type options loaded from backend */
  accountTypeOptions: LookupSelectOption[] = [];
  /** Raw lookup details — used for language-safe label resolution in getters */
  private accountTypeDetails: LookupDetail[] = [];

  // ── Specification filter fields ─────────────────────────

  availableFields: SpecFieldOption[] = [];

  readonly availableOperators: SpecOperatorOption[] = [
    { value: 'eq', label: 'COMMON.EQUALS' },
    { value: 'like', label: 'COMMON.CONTAINS' }
  ];

  showAdvancedFilters = false;
  activeSpecFilters: SpecFilter[] = [];

  // ── Tree filter state ─────────────────────────────────────

  quickFilterText = '';

  // ── Tree UI state ─────────────────────────────────────────

  expandedNodes = new Set<number>();
  selectedNodePk: number | null = null;

  // ── Inline edit state ─────────────────────────────────────

  editState: InlineEditState | null = null;

  // ── Inline add state ──────────────────────────────────────

  addState: InlineAddState | null = null;

  /** Map of account type codes to icon classes */
  readonly accountTypeIcons: Record<string, string> = {
    'ASSET':       'ti ti-building-bank',
    'LIABILITY':   'ti ti-receipt',
    'EQUITY':      'ti ti-shield-check',
    'REVENUE':     'ti ti-trending-up',
    'EXPENSE':     'ti ti-trending-down',
    'COGS':        'ti ti-package',
    'OTHER_COST':  'ti ti-file-invoice',
    'INTERNAL':    'ti ti-arrows-exchange',
    'STATISTICAL': 'ti ti-chart-bar'
  };

  get treeData(): AccountChartTreeNode[] { return this.facade.accountTree(); }
  get isLoading(): boolean { return this.facade.accountTreeLoading(); }
  get isSaving(): boolean { return this.facade.saving(); }

  /** Filtered tree data based on quick filter + spec filters */
  get filteredTreeData(): AccountChartTreeNode[] {
    let data = this.treeData;

    // Apply specification filters (client-side on tree)
    if (this.activeSpecFilters.length > 0) {
      data = this.applySpecFilters(data, this.activeSpecFilters);
    }

    // Apply quick filter
    const q = this.quickFilterText.trim().toLowerCase();
    if (q) {
      data = this.filterNodes(data, q);
    }

    return data;
  }

  /** Whether the edit-type dropdown is locked because node is a child (type inherited from parent) */
  get editTypeLockedByParent(): boolean {
    if (!this.editState) return false;
    const node = this.findNodeByPk(this.treeData, this.editState.pk);
    return node != null && node.parentPk !== null;
  }

  /** Resolved label for the currently edited node’s account type */
  get editAccountTypeLabel(): string {
    return this.resolveTypeLabel(this.editState?.accountType);
  }

  /** Resolved label for the account type being added (inherited from parent) */
  get addAccountTypeLabel(): string {
    return this.resolveTypeLabel(this.addState?.accountType);
  }

  /** Resolve account type label in the current language from raw lookup details */
  private resolveTypeLabel(typeValue: string | null | undefined): string {
    if (!typeValue) return '';
    const detail = this.accountTypeDetails.find(d => d.code === typeValue);
    if (detail) {
      return this.translate.currentLang === 'ar' ? detail.nameAr : (detail.nameEn || detail.nameAr);
    }
    // Fallback to pre-built options list (defensive)
    const option = this.accountTypeOptions.find(o => o.value === typeValue);
    return option ? option.label : typeValue;
  }

  /** Whether the edit-type dropdown should be disabled (has children OR has parent) */
  get editTypeDisabled(): boolean {
    if (!this.editState) return false;
    const node = this.findNodeByPk(this.treeData, this.editState.pk);
    if (!node) return false;
    return node.parentPk !== null || (node.children?.length ?? 0) > 0;
  }

  ngOnInit(): void {
    // Load account type options dynamically
    this.loadAccountTypeLookups();

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.loadAccountTypeLookups();
        this.cdr.detectChanges();
      });

    // Auto-load all accounts on page entry
    this.loadTree();
  }

  /**
   * Load account type options from backend and rebuild filter fields.
   */
  private loadAccountTypeLookups(): void {
    this.lookupService.getLookup(GL_LOOKUP_KEYS.ACCOUNT_TYPE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(details => {
        this.accountTypeDetails = details;
        const lang = this.translate.currentLang || 'ar';
        const options: LookupSelectOption[] = details.map(d => ({
          value: d.code,
          label: lang === 'ar' ? d.nameAr : (d.nameEn || d.nameAr)
        }));
        this.accountTypeOptions = options;
        this.availableFields = [
          { value: 'organizationFk', label: 'GL.ORGANIZATION' },
          { value: 'accountChartNo', label: 'GL.ACCOUNT_CODE' },
          { value: 'accountChartName', label: 'GL.ACCOUNT_NAME' },
          {
            value: 'accountType', label: 'GL.ACCOUNT_TYPE',
            options: options.map(o => ({ value: o.value, label: o.label }))
          },
          {
            value: 'isActive', label: 'COMMON.STATUS',
            options: [
              { value: true, label: 'COMMON.ACTIVE' },
              { value: false, label: 'COMMON.INACTIVE' }
            ]
          }
        ];
        this.cdr.detectChanges();
      });
  }

  // ── Keyboard shortcuts ────────────────────────────────────

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.editState) {
      this.cancelEdit();
    } else if (this.addState) {
      this.cancelAdd();
    }
  }

  // ── Tree loading ──────────────────────────────────────────

  loadTree(): void {
    // Preserve current expansion state across reload
    const preserveExpanded = this.treeData.length > 0;
    if (!preserveExpanded) {
      this.expandedNodes.clear();
    }
    this.selectedNodePk = null;
    this.editState = null;
    this.addState = null;
    this.facade.loadAccountTree();
  }

  // ── Tree navigation ───────────────────────────────────────

  toggleNode(pk: number): void {
    if (this.expandedNodes.has(pk)) {
      this.expandedNodes.delete(pk);
    } else {
      this.expandedNodes.add(pk);
    }
    this.cdr.detectChanges();
  }

  isExpanded(pk: number): boolean {
    return this.expandedNodes.has(pk);
  }

  selectNode(pk: number): void {
    this.selectedNodePk = this.selectedNodePk === pk ? null : pk;
    this.cdr.detectChanges();
  }

  isSelected(pk: number): boolean {
    return this.selectedNodePk === pk;
  }

  expandAll(): void {
    this.collectAllNodePks(this.treeData).forEach(pk => this.expandedNodes.add(pk));
    this.cdr.detectChanges();
  }

  collapseAll(): void {
    this.expandedNodes.clear();
    this.cdr.detectChanges();
  }

  getAccountTypeIcon(accountType: string): string {
    return this.accountTypeIcons[accountType] || 'ti ti-file-text';
  }

  // ── Specification Filter ──────────────────────────────────

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
    this.cdr.detectChanges();
  }

  onSpecFilterApply(filters: SpecFilter[]): void {
    this.activeSpecFilters = filters;
    this.cdr.detectChanges();
  }

  onSpecFilterClear(): void {
    this.activeSpecFilters = [];
    this.cdr.detectChanges();
  }

  // ── Inline Edit ───────────────────────────────────────────

  startEdit(node: AccountChartTreeNode): void {
    // Close any pending add
    this.addState = null;

    // If already editing another node, confirm discard
    if (this.editState && this.editState.pk !== node.accountChartPk && this.editState.dirty) {
      this.dialog.confirmDiscard().then(confirmed => {
        if (confirmed) {
          this.openEditPanel(node);
        }
      });
      return;
    }
    this.openEditPanel(node);
  }

  private openEditPanel(node: AccountChartTreeNode): void {
    this.editState = {
      pk: node.accountChartPk,
      accountChartName: node.accountChartName,
      accountType: node.accountType,
      isActive: node.isActive,
      dirty: false
    };
    this.selectedNodePk = node.accountChartPk;
    this.cdr.detectChanges();
  }

  onEditFieldChange(): void {
    if (this.editState) {
      this.editState.dirty = true;
      this.cdr.detectChanges();
    }
  }

  isEditing(pk: number): boolean {
    return this.editState?.pk === pk;
  }

  get editFormValid(): boolean {
    if (!this.editState) return false;
    return !!this.editState.accountChartName?.trim() && !!this.editState.accountType;
  }

  saveEdit(): void {
    if (!this.editState || !this.editFormValid) return;

    const node = this.findNodeByPk(this.treeData, this.editState.pk);
    if (!node) return;

    const request: UpdateAccountRequest = {
      accountChartName: this.editState.accountChartName.trim(),
      accountType: this.editState.accountType,
      organizationFk: node.organizationFk,
      isActive: this.editState.isActive,
      accountChartFk: node.parentPk
    };

    const pk = this.editState.pk;
    this.facade.updateAccountInTree(
      pk,
      request,
      undefined,
      undefined,
      () => {
        this.notification.success('GL.ACCOUNT_UPDATED');
        this.editState = null;
        this.cdr.detectChanges();
      }
    );
  }

  cancelEdit(): void {
    if (this.editState?.dirty) {
      this.dialog.confirmDiscard().then(confirmed => {
        if (confirmed) {
          this.editState = null;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.editState = null;
      this.cdr.detectChanges();
    }
  }

  // ── Inline Add ────────────────────────────────────────────

  startAddRoot(): void {
    this.editState = null;
    this.addState = {
      parentPk: null,
      organizationFk: 0,
      accountChartName: '',
      accountType: '',
      isActive: true
    };
    this.cdr.detectChanges();
  }

  startAddChild(parentNode: AccountChartTreeNode): void {
    this.editState = null;
    // Auto-expand parent if not expanded
    if (!this.expandedNodes.has(parentNode.accountChartPk)) {
      this.expandedNodes.add(parentNode.accountChartPk);
    }
    this.selectedNodePk = parentNode.accountChartPk;
    this.addState = {
      parentPk: parentNode.accountChartPk,
      organizationFk: parentNode.organizationFk,
      accountChartName: '',
      accountType: parentNode.accountType, // Inherit parent type
      isActive: true
    };
    this.cdr.detectChanges();
  }

  get addFormValid(): boolean {
    if (!this.addState) return false;
    return !!this.addState.accountChartName?.trim() && !!this.addState.accountType && !!this.addState.organizationFk;
  }

  saveAdd(): void {
    if (!this.addState || !this.addFormValid) return;

    const request: CreateAccountRequest = {
      accountChartName: this.addState.accountChartName.trim(),
      accountType: this.addState.accountType,
      organizationFk: this.addState.organizationFk,
      accountChartFk: this.addState.parentPk,
      isActive: this.addState.isActive
    };

    this.facade.createAccountInTree(
      request,
      undefined,
      undefined,
      (created) => {
        this.notification.success('GL.ACCOUNT_CREATED');
        this.addState = null;
        // Expand parent so new child is visible
        if (request.accountChartFk) {
          this.expandedNodes.add(request.accountChartFk);
        }
        this.selectedNodePk = created.accountChartPk;
        this.cdr.detectChanges();
      }
    );
  }

  cancelAdd(): void {
    this.addState = null;
    this.cdr.detectChanges();
  }

  /** Check if the add-child form should render under this node */
  isAddingChildOf(pk: number): boolean {
    return this.addState?.parentPk === pk;
  }

  /** Check if the add-root form should render */
  get isAddingRoot(): boolean {
    return this.addState != null && this.addState.parentPk === null;
  }

  // ── Status Toggle (Active / Inactive) ──────────────────────

  async toggleStatus(node: AccountChartTreeNode): Promise<void> {
    const isDeactivating = node.isActive;

    // Validation: cannot deactivate if has active children
    if (isDeactivating && this.hasActiveChildren(node)) {
      this.notification.warning('GL.CANNOT_DEACTIVATE_WITH_ACTIVE_CHILDREN');
      return;
    }

    const titleKey = isDeactivating ? 'GL.CONFIRM_DEACTIVATE_TITLE' : 'GL.CONFIRM_ACTIVATE_TITLE';
    const messageKey = isDeactivating ? 'GL.CONFIRM_DEACTIVATE_ACCOUNT' : 'GL.CONFIRM_ACTIVATE_ACCOUNT';
    const confirmKey = isDeactivating ? 'COMMON.DEACTIVATE' : 'COMMON.ACTIVATE';

    const confirmed = await this.dialog.confirm({
      titleKey,
      messageKey,
      messageParams: { name: `${node.accountChartNo} – ${node.accountChartName}` },
      type: isDeactivating ? 'danger' : 'info',
      confirmKey
    });

    if (!confirmed) return;

    const request: UpdateAccountRequest = {
      accountChartName: node.accountChartName,
      accountType: node.accountType,
      organizationFk: node.organizationFk,
      isActive: !node.isActive,
      accountChartFk: node.parentPk
    };

    this.facade.toggleAccountStatusInTree(
      node.accountChartPk,
      request,
      undefined,
      undefined,
      () => {
        const msgKey = isDeactivating ? 'GL.ACCOUNT_DEACTIVATED' : 'GL.ACCOUNT_ACTIVATED';
        this.notification.success(msgKey);
        if (this.editState?.pk === node.accountChartPk) {
          this.editState = null;
        }
        this.cdr.detectChanges();
      }
    );
  }

  /** Check if a node has any active children (recursive) */
  hasActiveChildren(node: AccountChartTreeNode): boolean {
    if (!node.children?.length) return false;
    for (const child of node.children) {
      if (child.isActive) return true;
      if (this.hasActiveChildren(child)) return true;
    }
    return false;
  }

  // ── Quick filter helpers ──────────────────────────────────

  private filterNodes(nodes: AccountChartTreeNode[], query: string): AccountChartTreeNode[] {
    const result: AccountChartTreeNode[] = [];
    for (const node of nodes) {
      const matchesSelf =
        node.accountChartName.toLowerCase().includes(query) ||
        node.accountChartNo.toLowerCase().includes(query);

      const filteredChildren = this.filterNodes(node.children || [], query);

      if (matchesSelf || filteredChildren.length > 0) {
        result.push({
          ...node,
          children: matchesSelf ? (node.children || []) : filteredChildren
        });
        // Auto-expand nodes that match via children
        if (filteredChildren.length > 0 && !matchesSelf) {
          this.expandedNodes.add(node.accountChartPk);
        }
      }
    }
    return result;
  }

  clearQuickFilter(): void {
    this.quickFilterText = '';
    this.cdr.detectChanges();
  }

  /** Apply specification filters recursively – keep node if it or any descendant matches */
  private applySpecFilters(nodes: AccountChartTreeNode[], filters: SpecFilter[]): AccountChartTreeNode[] {
    const result: AccountChartTreeNode[] = [];
    for (const node of nodes) {
      const matchesSelf = this.nodeMatchesSpecFilters(node, filters);
      const filteredChildren = this.applySpecFilters(node.children || [], filters);

      if (matchesSelf || filteredChildren.length > 0) {
        result.push({
          ...node,
          children: matchesSelf ? (node.children || []) : filteredChildren
        });
        if (filteredChildren.length > 0 && !matchesSelf) {
          this.expandedNodes.add(node.accountChartPk);
        }
      }
    }
    return result;
  }

  /** Check if a single node matches all spec filters */
  private nodeMatchesSpecFilters(node: AccountChartTreeNode, filters: SpecFilter[]): boolean {
    return filters.every(f => {
      if (f.value === null || f.value === undefined || f.value === '') return true;
      const val = String(f.value).toLowerCase();
      switch (f.field) {
        case 'organizationFk':
          return node.organizationFk === Number(f.value);
        case 'accountChartNo':
          return f.operator === 'like'
            ? node.accountChartNo.toLowerCase().includes(val)
            : node.accountChartNo.toLowerCase() === val;
        case 'accountChartName':
          return f.operator === 'like'
            ? node.accountChartName.toLowerCase().includes(val)
            : node.accountChartName.toLowerCase() === val;
        case 'accountType':
          return String(node.accountType) === String(f.value);
        case 'isActive':
          return node.isActive === (f.value === true || f.value === 'true');
        default:
          return true;
      }
    });
  }

  /** Whether any filters (quick or spec) are active */
  get hasActiveFilters(): boolean {
    return !!this.quickFilterText.trim() || this.activeSpecFilters.length > 0;
  }

  clearAllFilters(): void {
    this.quickFilterText = '';
    this.activeSpecFilters = [];
    this.cdr.detectChanges();
  }

  // ── Tree node helpers ─────────────────────────────────────

  findNodeByPk(nodes: AccountChartTreeNode[], pk: number): AccountChartTreeNode | null {
    for (const node of nodes) {
      if (node.accountChartPk === pk) return node;
      if (node.children?.length) {
        const found = this.findNodeByPk(node.children, pk);
        if (found) return found;
      }
    }
    return null;
  }

  private collectAllNodePks(nodes: AccountChartTreeNode[]): number[] {
    const pks: number[] = [];
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        pks.push(node.accountChartPk);
        pks.push(...this.collectAllNodePks(node.children));
      }
    }
    return pks;
  }

  /** Get the account type label for display */
  getAccountTypeLabelKey(typeValue: string): string {
    const found = this.accountTypeOptions.find(t => t.value === typeValue);
    return found ? found.label : '';
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
