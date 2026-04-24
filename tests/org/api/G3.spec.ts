/**
 * G3 — Region: CRUD
 * TC-ORG-001-11 … TC-ORG-001-14
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
const API_RG  = `${BASE}/api/organization/regions`;
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

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G3 — Region: CRUD', () => {
  let ctx:          APIRequestContext;
  let token:        string;
  let legalEntityId: number;
  let regionId:     number;
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

  // ── TC-ORG-001-11 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-11] Create region — HTTP 201, code starts with RGN-', async () => {
    const s = uid();
    const payload = {
      legalEntityId,
      regionNameAr: `منطقة-${s}`,
      regionNameEn: `Region-${s}`,
    };

    await test.step('POST /api/organization/regions', async () => {
      const resp = await ctx.post(API_RG, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-11', endpoint: 'POST /api/organization/regions', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(201);
      expect(body.data.regionCode, 'Region code must start with RGN-').toMatch(/^RGN-/);
      expect(body.data.legalEntityId, 'legalEntityId must match').toBe(legalEntityId);

      regionId = body.data.id as number;
      expect(regionId).toBeTruthy();
    });
  });

  // ── TC-ORG-001-12 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-12] List regions — HTTP 200, data array', async () => {
    await test.step('GET /api/organization/regions?page=0&pageSize=5', async () => {
      const resp = await ctx.get(`${API_RG}?page=0&pageSize=5`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-12', endpoint: 'GET /api/organization/regions', requestPayload: { page: 0, pageSize: 5 }, response: { count: body.data?.content?.length ?? body.data?.length }, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      const items = body.data?.content ?? body.data ?? [];
      expect(Array.isArray(items)).toBe(true);
    });
  });

  // ── TC-ORG-001-13 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-13] Get region by ID — HTTP 200, full object', async () => {
    test.skip(!regionId, 'Requires region from TC-ORG-001-11');

    await test.step(`GET /api/organization/regions/${regionId}`, async () => {
      const resp = await ctx.get(`${API_RG}/${regionId}`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-13', endpoint: `GET /api/organization/regions/${regionId}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.id).toBe(regionId);

      const dbResult = await validateInsert(ctx, API_RG, regionId, hdrs(token));
      expect(dbResult.validated, `DB read-back failed: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });

  // ── TC-ORG-001-14 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-14] Update region — HTTP 200, updated fields reflected', async () => {
    test.skip(!regionId, 'Requires region from TC-ORG-001-11');

    const updatedNameEn = `Updated-Region-${uid()}`;
    const payload = {
      legalEntityId,
      regionNameAr: `منطقة-محدثة-${uid()}`,
      regionNameEn: updatedNameEn,
    };

    await test.step(`PUT /api/organization/regions/${regionId}`, async () => {
      const resp = await ctx.put(`${API_RG}/${regionId}`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-14', endpoint: `PUT /api/organization/regions/${regionId}`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.regionNameEn).toBe(updatedNameEn);

      const dbResult = await validateUpdate(ctx, API_RG, regionId, { regionNameEn: updatedNameEn }, hdrs(token));
      expect(dbResult.validated, `DB update not reflected: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });
});
