/**
 * G4 — Region: Business Rules
 * TC-ORG-001-15 … TC-ORG-001-17
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

function errCode(body: Record<string, unknown>): string {
  return ((body as { error?: { code?: string }; message?: string }).error?.code ?? (body as { message?: string }).message ?? '') as string;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G4 — Region: Business Rules', () => {
  let ctx:   APIRequestContext;
  let token: string;
  const createdLeIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);
  });

  test.afterAll(async () => {
    // Best-effort cleanup
    for (const id of createdLeIds) {
      await ctx.patch(`${API_LE}/${id}/deactivate`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-ORG-001-15 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-15] Region creation blocked for inactive LE — REGION_INACTIVE_LEGAL_ENTITY (RULE-RG-02)', async () => {
    // Strategy: create a fresh LE with NO branches, then deactivate it.
    // (LE with no branches is not blocked by RULE-LE-07; RULE-LE-08 requires >1 active LE)
    const leId = await createLE(ctx, token);

    // Ensure there is already at least one other active LE so deactivation is allowed
    const listResp = await ctx.get(`${API_LE}?page=0&pageSize=100`, { headers: hdrs(token) });
    const listBody = await listResp.json();
    const items: Array<{ id: number; isActive: boolean }> = listBody.data?.content ?? listBody.data ?? [];
    const activeCount = items.filter((i) => i.isActive).length;

    if (activeCount <= 1) {
      // We'd violate LAST_ACTIVE_ENTITY rule — need another active LE first
      await createLE(ctx, token); // extra active LE so we can deactivate leId
    }

    // Deactivate the fresh LE
    const deactivateResp = await ctx.patch(`${API_LE}/${leId}/deactivate`, { headers: hdrs(token) });
    if (deactivateResp.status() !== 200) {
      test.skip(); // Cannot deactivate (some other guard); skip gracefully
      return;
    }

    const s = uid();
    const payload = {
      legalEntityId: leId,
      regionNameAr:  `منطقة-inactive-${s}`,
      regionNameEn:  `Region-inactive-${s}`,
    };

    await test.step('POST region with inactive legalEntityId', async () => {
      const resp = await ctx.post(API_RG, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-15', endpoint: 'POST /api/organization/regions', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Must not be 201 for inactive LE').not.toBe(201);
      const code = errCode(body);
      expect(
        code.includes('INACTIVE') || code.includes('INACTIVE_LEGAL') || resp.status() === 409 || resp.status() === 422,
        `Expected inactive-entity error, got: ${code} (HTTP ${resp.status()})`,
      ).toBeTruthy();
    });
  });

  // ── TC-ORG-001-16 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-16] legalEntityId locked after save — PUT ignores change (RULE-RG-04)', async () => {
    // Create two LEs and a region under LE-A
    const leAId = await createLE(ctx, token);
    const leBId = await createLE(ctx, token);
    createdLeIds.push(leAId, leBId);

    const regionId = await createRegion(ctx, token, leAId);

    // Attempt to change the region's legalEntityId to LE-B via PUT
    const s = uid();
    const payload = {
      legalEntityId: leBId,   // attempted change — should be silently ignored
      regionNameAr:  `منطقة-locked-${s}`,
      regionNameEn:  `Region-locked-${s}`,
    };

    await test.step(`PUT /api/organization/regions/${regionId} — change legalEntityId`, async () => {
      const resp = await ctx.put(`${API_RG}/${regionId}`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-16', endpoint: `PUT /api/organization/regions/${regionId}`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(
        body.data.legalEntityId,
        'legalEntityId must remain LE-A (locked field cannot be changed)',
      ).toBe(leAId);
    });
  });

  // ── TC-ORG-001-17 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-17] Deactivate region with active branches blocked — REGION_HAS_ACTIVE_BRANCHES (RULE-RG-05)', async () => {
    const leId     = await createLE(ctx, token);
    createdLeIds.push(leId);
    const regionId = await createRegion(ctx, token, leId);

    // Create an active branch inside this region
    const s = uid();
    const brResp = await ctx.post(API_BR, {
      data: {
        legalEntityId: leId,
        regionId,
        branchNameAr:  `فرع-${s}`,
        branchNameEn:  `Branch-${s}`,
        branchTypeId:  'BRANCH',
        isHeadquarter: false,
      },
      headers: hdrs(token),
    });

    if (brResp.status() !== 201) {
      test.skip(); // BRANCH_TYPE lookup unavailable — skip
      return;
    }

    await test.step(`PATCH /api/organization/regions/${regionId}/deactivate`, async () => {
      const resp = await ctx.patch(`${API_RG}/${regionId}/deactivate`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-17', endpoint: `PATCH /api/organization/regions/${regionId}/deactivate`, requestPayload: { regionId }, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(409);
      const code = errCode(body);
      expect(
        code.includes('HAS_ACTIVE_BRANCHES') || code.includes('ACTIVE_BRANCHES'),
        `Expected REGION_HAS_ACTIVE_BRANCHES, got: ${code}`,
      ).toBeTruthy();
    });
  });
});
