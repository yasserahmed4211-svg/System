/**
 * G7 — Department: CRUD
 * TC-ORG-001-27 … TC-ORG-001-29
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

const BASE    = 'http://localhost:7272';
const API_LE  = `${BASE}/api/organization/legal-entities`;
const API_BR  = `${BASE}/api/organization/branches`;
const API_DEP = `${BASE}/api/organization/departments`;
const TENANT  = 'default';

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

async function createBranch(ctx: APIRequestContext, token: string, leId: number): Promise<number> {
  const s = uid();
  const resp = await ctx.post(API_BR, {
    data: { legalEntityId: leId, branchNameAr: `فرع-${s}`, branchNameEn: `Branch-${s}`, branchTypeId: BRANCH_TYPE, isHeadquarter: false },
    headers: hdrs(token),
  });
  const body = await resp.json();
  expect(resp.status(), `Seed Branch failed: ${JSON.stringify(body)}`).toBe(201);
  return body.data.id as number;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G7 — Department: CRUD', () => {
  let ctx:          APIRequestContext;
  let token:        string;
  let legalEntityId: number;
  let branchId:     number;
  let departmentId: number;
  const createdLeIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx           = await playwright.request.newContext({ baseURL: BASE });
    token         = await getAdminToken(ctx);
    legalEntityId = await createLE(ctx, token);
    createdLeIds.push(legalEntityId);
    branchId = await createBranch(ctx, token, legalEntityId);
  });

  test.afterAll(async () => {
    for (const id of createdLeIds) {
      await ctx.patch(`${API_LE}/${id}/deactivate`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-ORG-001-27 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-27] Add standalone department to branch — HTTP 201, code DEP-*', async () => {
    const s = uid();
    const payload = {
      branchId,
      departmentNameAr: `قسم-${s}`,
      departmentNameEn: `Dept-${s}`,
      departmentTypeId: DEPT_TYPE,
    };

    await test.step('POST /api/organization/departments', async () => {
      const resp = await ctx.post(API_DEP, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-27', endpoint: 'POST /api/organization/departments', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(201);
      expect(body.data.departmentCode, 'Department code must start with DEP-').toMatch(/^DEP-/);
      expect(body.data.branchId, 'branchId must match').toBe(branchId);

      departmentId = body.data.id as number;
      expect(departmentId).toBeTruthy();
    });
  });

  // ── TC-ORG-001-28 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-28] List departments by branch — HTTP 200, data array', async () => {
    test.skip(!branchId, 'Requires branch from beforeAll');

    await test.step(`GET /api/organization/branches/${branchId}/departments`, async () => {
      const resp = await ctx.get(`${API_BR}/${branchId}/departments`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-28', endpoint: `GET /api/organization/branches/${branchId}/departments`, requestPayload: { branchId }, response: { count: body.data?.length }, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      const items = body.data ?? [];
      expect(Array.isArray(items), 'data must be an array').toBe(true);
    });
  });

  // ── TC-ORG-001-29 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-29] Update department — HTTP 200, updated fields reflected', async () => {
    test.skip(!departmentId, 'Requires department from TC-ORG-001-27');

    const updatedNameEn = `Updated-Dept-${uid()}`;
    const payload = {
      branchId,
      departmentNameAr: `قسم-محدث-${uid()}`,
      departmentNameEn: updatedNameEn,
      departmentTypeId: DEPT_TYPE,
    };

    await test.step(`PUT /api/organization/departments/${departmentId}`, async () => {
      const resp = await ctx.put(`${API_DEP}/${departmentId}`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-29', endpoint: `PUT /api/organization/departments/${departmentId}`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.departmentNameEn).toBe(updatedNameEn);

      const dbResult = await validateUpdate(ctx, API_DEP, departmentId, { departmentNameEn: updatedNameEn }, hdrs(token));
      expect(dbResult.validated, `DB update not reflected: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });
});
