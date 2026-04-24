/**
 * G5 — Branch: CRUD
 * TC-ORG-001-18 … TC-ORG-001-22
 *
 * Source : ai-governance/01-modules/ORG/ORG-001-organization-module/05-tests/test-cases.md
 * Layer  : Backend API
 * Author : Playwright Expert Agent (auto-generated 2026-04-23)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';
import { validateInsert, validateUpdate } from '../db/db.helper';

test.use({
  trace: 'retain-on-failure',
  screenshot: 'only-on-failure',
  video: 'retain-on-failure',
});

const BASE   = 'http://localhost:7272';
const API_LE = `${BASE}/api/organization/legal-entities`;
const API_RG = `${BASE}/api/organization/regions`;
const API_BR = `${BASE}/api/organization/branches`;
const TENANT = 'default';

const COUNTRY_FK  = 1;
const CURRENCY_FK = 1;
const BRANCH_TYPE = 'BRANCH';
const DEPT_TYPE   = 'SALES';

function uid(): string {
  return `T${Date.now().toString(36).toUpperCase()}_${Math.random().toString(36).slice(2, 5).toUpperCase()}`;
}

function hdrs(token: string): Record<string, string> {
  return {
    Authorization:  `Bearer ${token}`,
    'X-Tenant-Id':  TENANT,
    'Content-Type': 'application/json',
  };
}

async function getAdminToken(ctx: APIRequestContext): Promise<string> {
  const resp = await ctx.post(`${BASE}/api/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
    headers: { 'X-Tenant-Id': TENANT },
  });
  expect(resp.ok()).toBeTruthy();
  const body = await resp.json();
  return (body.data?.accessToken ?? body.accessToken) as string;
}

async function createLE(ctx: APIRequestContext, token: string): Promise<number> {
  const s = uid();
  const resp = await ctx.post(API_LE, {
    data: { legalEntityNameAr: `كيان-${s}`, legalEntityNameEn: `Entity-${s}`, countryId: COUNTRY_FK, functionalCurrencyId: CURRENCY_FK },
    headers: hdrs(token),
  });
  const body = await resp.json();
  expect(resp.status(), `Seed LE failed: ${JSON.stringify(body)}`).toBe(201);
  return body.data.id as number;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G5 — Branch: CRUD', () => {
  let ctx:          APIRequestContext;
  let token:        string;
  let legalEntityId: number;
  let branchId:     number;
  let branchWithDeptsId: number;
  const createdLeIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx           = await playwright.request.newContext({ baseURL: BASE });
    token         = await getAdminToken(ctx);
    legalEntityId = await createLE(ctx, token);
    createdLeIds.push(legalEntityId);
  });

  test.afterAll(async () => {
    for (const id of createdLeIds) {
      await ctx.patch(`${API_LE}/${id}/deactivate`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-ORG-001-18 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-18] Create branch (no departments) — HTTP 201, code BRN-*, isActive=true', async () => {
    const s = uid();
    const payload = {
      legalEntityId,
      branchNameAr:  `فرع-${s}`,
      branchNameEn:  `Branch-${s}`,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: false,
    };

    await test.step('POST /api/organization/branches', async () => {
      const resp = await ctx.post(API_BR, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-18', endpoint: 'POST /api/organization/branches', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(201);
      expect(body.data.branchCode, 'Branch code must start with BRN-').toMatch(/^BRN-/);
      expect(body.data.isActive, 'isActive must be true').toBe(true);

      branchId = body.data.id as number;
      expect(branchId).toBeTruthy();
    });
  });

  // ── TC-ORG-001-19 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-19] Create branch with inline departments atomically — HTTP 201, departments DEP-*', async () => {
    const s = uid();
    const payload = {
      legalEntityId,
      branchNameAr:  `فرع-dept-${s}`,
      branchNameEn:  `Branch-dept-${s}`,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: false,
      departments: [
        { departmentNameAr: `قسم-A-${s}`, departmentNameEn: `Dept-A-${s}`, departmentTypeId: DEPT_TYPE },
        { departmentNameAr: `قسم-B-${s}`, departmentNameEn: `Dept-B-${s}`, departmentTypeId: DEPT_TYPE },
      ],
    };

    await test.step('POST /api/organization/branches with inline departments[]', async () => {
      const resp = await ctx.post(API_BR, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-19', endpoint: 'POST /api/organization/branches', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(201);

      const depts: Array<{ departmentCode: string }> = body.data.departments ?? [];
      expect(depts.length, 'Must have 2 inline departments').toBe(2);
      for (const d of depts) {
        expect(d.departmentCode, 'Dept code must start with DEP-').toMatch(/^DEP-/);
      }

      branchWithDeptsId = body.data.id as number;
    });
  });

  // ── TC-ORG-001-20 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-20] List branches — HTTP 200, paginated data array', async () => {
    await test.step('GET /api/organization/branches?page=0&pageSize=5', async () => {
      const resp = await ctx.get(`${API_BR}?page=0&pageSize=5`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-20', endpoint: 'GET /api/organization/branches', requestPayload: { page: 0, pageSize: 5 }, response: { count: body.data?.content?.length ?? body.data?.length }, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      const items = body.data?.content ?? body.data ?? [];
      expect(Array.isArray(items)).toBe(true);
    });
  });

  // ── TC-ORG-001-21 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-21] Get branch by ID — HTTP 200, departments[] present', async () => {
    test.skip(!branchWithDeptsId, 'Requires branch from TC-ORG-001-19');

    await test.step(`GET /api/organization/branches/${branchWithDeptsId}`, async () => {
      const resp = await ctx.get(`${API_BR}/${branchWithDeptsId}`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-21', endpoint: `GET /api/organization/branches/${branchWithDeptsId}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.id).toBe(branchWithDeptsId);
      expect(Array.isArray(body.data.departments), 'departments must be an array').toBe(true);

      const dbResult = await validateInsert(ctx, API_BR, branchWithDeptsId, hdrs(token));
      expect(dbResult.validated, `DB read-back failed: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });

  // ── TC-ORG-001-22 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-22] Update branch — HTTP 200, updated fields reflected', async () => {
    test.skip(!branchId, 'Requires branch from TC-ORG-001-18');

    const updatedNameEn = `Updated-Branch-${uid()}`;
    const payload = {
      legalEntityId,
      branchNameAr:  `فرع-محدث-${uid()}`,
      branchNameEn:  updatedNameEn,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: false,
    };

    await test.step(`PUT /api/organization/branches/${branchId}`, async () => {
      const resp = await ctx.put(`${API_BR}/${branchId}`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-22', endpoint: `PUT /api/organization/branches/${branchId}`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.branchNameEn).toBe(updatedNameEn);

      const dbResult = await validateUpdate(ctx, API_BR, branchId, { branchNameEn: updatedNameEn }, hdrs(token));
      expect(dbResult.validated, `DB update not reflected: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });
});
