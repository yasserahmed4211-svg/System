/**
 * G6 — Branch: Business Rules
 * TC-ORG-001-23 … TC-ORG-001-26
 *
 * Source : ai-governance/01-modules/ORG/ORG-001-organization-module/05-tests/test-cases.md
 * Layer  : Backend API
 * Author : Playwright Expert Agent (auto-generated 2026-04-23)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';

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

async function createRegion(ctx: APIRequestContext, token: string, leId: number): Promise<number> {
  const s = uid();
  const resp = await ctx.post(API_RG, {
    data: { legalEntityId: leId, regionNameAr: `منطقة-${s}`, regionNameEn: `Region-${s}` },
    headers: hdrs(token),
  });
  const body = await resp.json();
  expect(resp.status(), `Seed Region failed: ${JSON.stringify(body)}`).toBe(201);
  return body.data.id as number;
}

async function createBranch(ctx: APIRequestContext, token: string, leId: number, isHQ: boolean, rgId?: number): Promise<number> {
  const s = uid();
  const resp = await ctx.post(API_BR, {
    data: {
      legalEntityId: leId,
      regionId: rgId,
      branchNameAr:  `فرع-${s}`,
      branchNameEn:  `Branch-${s}`,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: isHQ,
    },
    headers: hdrs(token),
  });
  const body = await resp.json();
  expect(resp.status(), `Seed Branch failed: ${JSON.stringify(body)}`).toBe(201);
  return body.data.id as number;
}

function errCode(body: Record<string, unknown>): string {
  return ((body as { error?: { code?: string }; message?: string }).error?.code ?? (body as { message?: string }).message ?? '') as string;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G6 — Branch: Business Rules', () => {
  let ctx:   APIRequestContext;
  let token: string;
  const createdLeIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);
  });

  test.afterAll(async () => {
    for (const id of createdLeIds) {
      await ctx.patch(`${API_LE}/${id}/deactivate`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-ORG-001-23 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-23] Duplicate HQ same LE blocked — BRANCH_HQ_EXISTS (RULE-BR-05)', async () => {
    const leId = await createLE(ctx, token);
    createdLeIds.push(leId);

    // Create first HQ branch
    await createBranch(ctx, token, leId, true);

    // Attempt to create a second HQ for the same LE
    const s = uid();
    const payload = {
      legalEntityId: leId,
      branchNameAr:  `فرع-hq2-${s}`,
      branchNameEn:  `Branch-hq2-${s}`,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: true,
    };

    await test.step('POST second HQ branch for same LE', async () => {
      const resp = await ctx.post(API_BR, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-23', endpoint: 'POST /api/organization/branches', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not be 201 — duplicate HQ').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('HQ_EXISTS') || code.includes('BRANCH_HQ'),
        `Expected BRANCH_HQ_EXISTS, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-24 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-24] Region from different LE blocked — BRANCH_REGION_MISMATCH (RULE-BR-04)', async () => {
    // Create two separate LEs
    const leAId = await createLE(ctx, token);
    const leBId = await createLE(ctx, token);
    createdLeIds.push(leAId, leBId);

    // Region belongs to LE-B
    const regionBId = await createRegion(ctx, token, leBId);

    // Try to create a branch under LE-A but with a region from LE-B
    const s = uid();
    const payload = {
      legalEntityId: leAId,        // branch belongs to LE-A
      regionId:      regionBId,    // but region belongs to LE-B — mismatch
      branchNameAr:  `فرع-mismatch-${s}`,
      branchNameEn:  `Branch-mismatch-${s}`,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: false,
    };

    await test.step('POST branch with cross-LE regionId', async () => {
      const resp = await ctx.post(API_BR, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-24', endpoint: 'POST /api/organization/branches', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not be 201 — region mismatch').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('MISMATCH') || code.includes('REGION') || resp.status() === 409 || resp.status() === 422,
        `Expected REGION_MISMATCH, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-25 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-25] Atomic rollback — invalid dept causes full rollback — PARTIAL_SAVE_FAILED (RULE-BR-08)', async () => {
    const leId = await createLE(ctx, token);
    createdLeIds.push(leId);

    const s = uid();
    const payload = {
      legalEntityId: leId,
      branchNameAr:  `فرع-atomic-${s}`,
      branchNameEn:  `Branch-atomic-${s}`,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: false,
      departments: [
        { departmentNameAr: `قسم-valid-${s}`, departmentNameEn: `Dept-valid-${s}`, departmentTypeId: DEPT_TYPE },
        { departmentNameAr: '', departmentNameEn: '', departmentTypeId: DEPT_TYPE }, // invalid — empty names
      ],
    };

    await test.step('POST branch with one invalid inline department', async () => {
      const resp = await ctx.post(API_BR, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-25', endpoint: 'POST /api/organization/branches', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not be 201 for partial invalid departments').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('PARTIAL_SAVE') || code.includes('ROLLBACK') || resp.status() === 400 || resp.status() === 422,
        `Expected PARTIAL_SAVE_FAILED or 400/422, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-26 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-26] Invalid branch email rejected — BRANCH_INVALID_EMAIL (RULE-BR-06)', async () => {
    const leId = await createLE(ctx, token);
    createdLeIds.push(leId);

    const s = uid();
    const payload = {
      legalEntityId: leId,
      branchNameAr:  `فرع-email-${s}`,
      branchNameEn:  `Branch-email-${s}`,
      branchTypeId:  BRANCH_TYPE,
      isHeadquarter: false,
      email:         'notvalid@',
    };

    await test.step('POST branch with invalid email', async () => {
      const resp = await ctx.post(API_BR, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-26', endpoint: 'POST /api/organization/branches', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not be 201 for invalid email').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('INVALID_EMAIL') || code.includes('EMAIL') || resp.status() === 400,
        `Expected INVALID_EMAIL, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });
});
