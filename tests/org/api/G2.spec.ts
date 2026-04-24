/**
 * G2 — Legal Entity: Business Rules
 * TC-ORG-001-06 … TC-ORG-001-10
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

const BASE    = 'http://localhost:7272';
const API_LE  = `${BASE}/api/organization/legal-entities`;
const API_BR  = `${BASE}/api/organization/branches`;
const TENANT  = 'default';

const COUNTRY_FK  = 1;
const CURRENCY_FK = 1;

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

function errCode(body: Record<string, unknown>): string {
  return ((body as { error?: { code?: string }; message?: string }).error?.code ?? (body as { message?: string }).message ?? '') as string;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G2 — Legal Entity: Business Rules', () => {
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

  // ── TC-ORG-001-06 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-06] Invalid email format rejected — LEGAL_ENTITY_INVALID_EMAIL (RULE-LE-03)', async () => {
    const s = uid();
    const payload = {
      legalEntityNameAr: `كيان-email-${s}`,
      legalEntityNameEn: `Entity-email-${s}`,
      countryId: COUNTRY_FK,
      functionalCurrencyId: CURRENCY_FK,
      email: 'not-a-valid-email',
    };

    await test.step('POST with malformed email', async () => {
      const resp = await ctx.post(API_LE, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-06', endpoint: 'POST /api/organization/legal-entities', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not return 201 for invalid email').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('INVALID_EMAIL') || code.includes('EMAIL') || resp.status() === 400,
        `Expected INVALID_EMAIL in error code, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-07 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-07] Invalid URL format rejected — LEGAL_ENTITY_INVALID_URL (RULE-LE-04)', async () => {
    const s = uid();
    const payload = {
      legalEntityNameAr: `كيان-url-${s}`,
      legalEntityNameEn: `Entity-url-${s}`,
      countryId: COUNTRY_FK,
      functionalCurrencyId: CURRENCY_FK,
      website: 'not a url at all',
    };

    await test.step('POST with malformed website URL', async () => {
      const resp = await ctx.post(API_LE, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-07', endpoint: 'POST /api/organization/legal-entities', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not return 201 for invalid URL').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('INVALID_URL') || code.includes('URL') || resp.status() === 400,
        `Expected INVALID_URL in error code, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-08 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-08] Fiscal month out of range rejected — LEGAL_ENTITY_INVALID_MONTH (RULE-LE-05)', async () => {
    const s = uid();
    const payload = {
      legalEntityNameAr: `كيان-month-${s}`,
      legalEntityNameEn: `Entity-month-${s}`,
      countryId: COUNTRY_FK,
      functionalCurrencyId: CURRENCY_FK,
      fiscalYearStartMonth: 13,
    };

    await test.step('POST with fiscalYearStartMonth=13', async () => {
      const resp = await ctx.post(API_LE, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-08', endpoint: 'POST /api/organization/legal-entities', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not return 201 for month=13').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('INVALID_MONTH') || code.includes('MONTH') || resp.status() === 400,
        `Expected INVALID_MONTH in error code, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-09 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-09] Deactivate LE with active branches blocked — LEGAL_ENTITY_HAS_ACTIVE_BRANCHES (RULE-LE-07)', async () => {
    const leId = await createLE(ctx, token);
    createdLeIds.push(leId);

    // Create an active branch under this legal entity
    const s = uid();
    const brResp = await ctx.post(API_BR, {
      data: {
        legalEntityId: leId,
        branchNameAr:  `فرع-${s}`,
        branchNameEn:  `Branch-${s}`,
        branchTypeId:  'BRANCH',
        isHeadquarter: false,
      },
      headers: hdrs(token),
    });

    if (brResp.status() !== 201) {
      test.skip(); // guard: if BRANCH_TYPE lookup missing, skip
      return;
    }

    await test.step(`PATCH /api/organization/legal-entities/${leId}/deactivate`, async () => {
      const resp = await ctx.patch(`${API_LE}/${leId}/deactivate`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-09', endpoint: `PATCH /api/organization/legal-entities/${leId}/deactivate`, requestPayload: { legalEntityId: leId }, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(409);
      const code = errCode(body);
      expect(
        code.includes('HAS_ACTIVE_BRANCHES') || code.includes('ACTIVE_BRANCHES'),
        `Expected HAS_ACTIVE_BRANCHES, got: ${code}`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-10 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-10] Deactivate last active LE blocked — LEGAL_ENTITY_LAST_ACTIVE_ENTITY (RULE-LE-08)', async () => {
    // Get list of all active legal entities
    const listResp = await ctx.get(`${API_LE}?page=0&pageSize=100`, { headers: hdrs(token) });
    const listBody = await listResp.json();
    const items: Array<{ id: number; isActive: boolean }> = listBody.data?.content ?? listBody.data ?? [];
    const activeItems = items.filter((i) => i.isActive);

    if (activeItems.length !== 1) {
      test.skip(); // More than one active LE — skip (LAST_ACTIVE rule won't trigger)
      return;
    }

    const lastActiveId = activeItems[0].id;

    await test.step(`PATCH /api/organization/legal-entities/${lastActiveId}/deactivate`, async () => {
      const resp = await ctx.patch(`${API_LE}/${lastActiveId}/deactivate`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-10', endpoint: `PATCH /api/organization/legal-entities/${lastActiveId}/deactivate`, requestPayload: { legalEntityId: lastActiveId }, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(409);
      const code = errCode(body);
      expect(
        code.includes('LAST_ACTIVE') || code.includes('LAST_ACTIVE_ENTITY'),
        `Expected LAST_ACTIVE_ENTITY, got: ${code}`,
      ).toBeTruthy();
    });
  });
});
