import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  AccountChartDto,
  AccountChartTreeNode,
  AccRuleHdrDto,
  CreateAccountRequest,
  UpdateAccountRequest,
  CreateRuleRequest,
  UpdateRuleRequest,
  EligibleParentAccountDto,
  PagedResponse,
  SearchRequest
} from 'src/app/modules/finance/gl/models/gl.model';

/**
 * GL API Service
 *
 * Communicates with the GL backend endpoints as defined in gl.contract.md.
 * Extends BaseApiService for unwrapping and typed HTTP helpers.
 *
 * @requirement FE-REQ-GL-001
 * @task TASK-FE-GL-001
 */
@Injectable()
export class GlApiService extends BaseApiService {
  private readonly accountsUrl = `${environment.authApiUrl}/api/gl/accounts`;
  private readonly rulesUrl = `${environment.authApiUrl}/api/gl/rules`;

  // ── Chart of Accounts ──────────────────────────────────────

  searchAccounts(request: SearchRequest): Observable<PagedResponse<AccountChartDto>> {
    return this.doPost<PagedResponse<AccountChartDto>>(`${this.accountsUrl}/search`, request);
  }

  getAccountById(pk: number): Observable<AccountChartDto> {
    return this.doGet<AccountChartDto>(`${this.accountsUrl}/${pk}`);
  }

  createAccount(request: CreateAccountRequest): Observable<AccountChartDto> {
    return this.doPost<AccountChartDto>(this.accountsUrl, request);
  }

  updateAccount(pk: number, request: UpdateAccountRequest): Observable<AccountChartDto> {
    return this.doPut<AccountChartDto>(`${this.accountsUrl}/${pk}`, request);
  }

  deactivateAccount(pk: number): Observable<void> {
    return this.doDelete<void>(`${this.accountsUrl}/${pk}`);
  }

  getAccountTree(organizationFk?: number, accountType?: string): Observable<AccountChartTreeNode[]> {
    let httpParams = new HttpParams();
    if (organizationFk !== undefined) httpParams = httpParams.set('organizationFk', String(organizationFk));
    if (accountType !== undefined) httpParams = httpParams.set('accountType', String(accountType));
    return this.doGet<AccountChartTreeNode[]>(`${this.accountsUrl}/tree`, httpParams);
  }

  getAccountSubTree(accountChartPk: number): Observable<AccountChartTreeNode> {
    return this.doGet<AccountChartTreeNode>(`${this.accountsUrl}/tree/${accountChartPk}`);
  }

  /**
   * Returns eligible parent accounts for the LOV (erp-dual-list).
   * Excludes the given account and its descendants to prevent circular references.
   */
  getEligibleParents(params: {
    organizationFk: number;
    excludeAccountPk?: number;
    search?: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<EligibleParentAccountDto>> {
    let httpParams = new HttpParams()
      .set('organizationFk', String(params.organizationFk));
    if (params.excludeAccountPk !== undefined) {
      httpParams = httpParams.set('excludeAccountPk', String(params.excludeAccountPk));
    }
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.page !== undefined) httpParams = httpParams.set('page', String(params.page));
    if (params.size !== undefined) httpParams = httpParams.set('size', String(params.size));

    return this.doGet<PagedResponse<EligibleParentAccountDto>>(
      `${this.accountsUrl}/eligible-parents`, httpParams);
  }

  // ── Accounting Rules ───────────────────────────────────────

  searchRules(request: SearchRequest): Observable<PagedResponse<AccRuleHdrDto>> {
    return this.doPost<PagedResponse<AccRuleHdrDto>>(`${this.rulesUrl}/search`, request);
  }

  getRuleById(ruleId: number): Observable<AccRuleHdrDto> {
    return this.doGet<AccRuleHdrDto>(`${this.rulesUrl}/${ruleId}`);
  }

  createRule(request: CreateRuleRequest): Observable<AccRuleHdrDto> {
    return this.doPost<AccRuleHdrDto>(this.rulesUrl, request);
  }

  updateRule(ruleId: number, request: UpdateRuleRequest): Observable<AccRuleHdrDto> {
    return this.doPut<AccRuleHdrDto>(`${this.rulesUrl}/${ruleId}`, request);
  }

  deactivateRule(ruleId: number): Observable<AccRuleHdrDto> {
    return this.http.patch<unknown>(`${this.rulesUrl}/${ruleId}/deactivate`, {}).pipe(
      map(res => this.unwrapResponse<AccRuleHdrDto>(res)),
      catchError(err => throwError(() => err))
    );
  }
}
