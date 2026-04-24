/**
 * G1 — Legal Entity: CRUD
 * TC-ORG-001-01 … TC-ORG-001-05
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

const BASE = 'http://localhost:7272';
const API_LE = `${BASE}/api/organization/legal-entities`;
const TENANT = 'default';

// Seeded lookup IDs (from V2__baseline_data.sql seed)
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
  expect(resp.ok(), 'Login must succeed').toBeTruthy();
  const body = await resp.json();
  const token = (body.data?.accessToken ?? body.accessToken) as string;
  expect(token, 'accessToken must be present').toBeTruthy();
  return token;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G1 — Legal Entity: CRUD', () => {
  let ctx:   APIRequestContext;
  let token: string;
  const createdIds: number[] = [];
  let legalEntityId: number;

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);
  });

  test.afterAll(async () => {
    // Cleanup — deactivate created entities (no hard-delete per B3.2)
    for (const id of createdIds) {
      await ctx.patch(`${API_LE}/${id}/deactivate`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-ORG-001-01 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-01] Create legal entity — success (HTTP 201, code LE-*, isActive=true)', async () => {
    const s = uid();
    const payload = {
      legalEntityNameAr: `كيان-${s}`,
      legalEntityNameEn: `Entity-${s}`,
      countryId: COUNTRY_FK,
      functionalCurrencyId: CURRENCY_FK,
    };

    await test.step('POST /api/organization/legal-entities', async () => {
      const resp = await ctx.post(API_LE, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-01', endpoint: 'POST /api/organization/legal-entities', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Expected HTTP 201').toBe(201);
      expect(body.data, 'Response must contain data').toBeDefined();
      expect(body.data.legalEntityCode, 'Code must start with LE-').toMatch(/^LE-/);
      expect(body.data.isActive, 'isActive must be true on create').toBe(true);

      legalEntityId = body.data.id as number;
      expect(legalEntityId, 'id must be present').toBeTruthy();
      createdIds.push(legalEntityId);
    });
  });

  // ── TC-ORG-001-02 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-02] List legal entities — paginated (HTTP 200, data array)', async () => {
    await test.step('GET /api/organization/legal-entities?page=0&pageSize=5', async () => {
      const resp = await ctx.get(`${API_LE}?page=0&pageSize=5`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-02', endpoint: 'GET /api/organization/legal-entities', requestPayload: { page: 0, pageSize: 5 }, response: { status: body.status, count: body.data?.content?.length ?? body.data?.length }, statusCode: resp.status() });

      expect(resp.status(), 'Expected HTTP 200').toBe(200);
      // Accept Spring Page wrapper (body.data.content) or flat array
      const items = body.data?.content ?? body.data ?? [];
      expect(Array.isArray(items), 'data must be an array').toBe(true);
    });
  });

  // ── TC-ORG-001-03 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-03] Get legal entity by ID — HTTP 200, full object', async () => {
    test.skip(!legalEntityId, 'Requires legal entity created in TC-ORG-001-01');

    await test.step(`GET /api/organization/legal-entities/${legalEntityId}`, async () => {
      const resp = await ctx.get(`${API_LE}/${legalEntityId}`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-03', endpoint: `GET /api/organization/legal-entities/${legalEntityId}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.id).toBe(legalEntityId);
      expect(body.data.legalEntityCode).toMatch(/^LE-/);

      // DB validation via read-back
      const dbResult = await validateInsert(ctx, API_LE, legalEntityId, hdrs(token));
      expect(dbResult.validated, `DB read-back failed: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });

  // ── TC-ORG-001-04 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-04] Update legal entity — HTTP 200, updated fields reflected', async () => {
    test.skip(!legalEntityId, 'Requires legal entity created in TC-ORG-001-01');

    const updatedNameEn = `Updated-Entity-${uid()}`;
    const payload = {
      legalEntityNameAr: `كيان-محدث-${uid()}`,
      legalEntityNameEn: updatedNameEn,
      countryId: COUNTRY_FK,
      functionalCurrencyId: CURRENCY_FK,
    };

    await test.step(`PUT /api/organization/legal-entities/${legalEntityId}`, async () => {
      const resp = await ctx.put(`${API_LE}/${legalEntityId}`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-04', endpoint: `PUT /api/organization/legal-entities/${legalEntityId}`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.legalEntityNameEn).toBe(updatedNameEn);

      // DB validation
      const dbResult = await validateUpdate(ctx, API_LE, legalEntityId, { legalEntityNameEn: updatedNameEn }, hdrs(token));
      expect(dbResult.validated, `DB update not reflected: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });

  // ── TC-ORG-001-05 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-05] System fields (createdAt, createdBy) auto-populated on create', async () => {
    const s = uid();
    const payload = {
      legalEntityNameAr: `كيان-sys-${s}`,
      legalEntityNameEn: `Entity-sys-${s}`,
      countryId: COUNTRY_FK,
      functionalCurrencyId: CURRENCY_FK,
    };

    await test.step('POST and verify system fields', async () => {
      const resp = await ctx.post(API_LE, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-ORG-001-05', endpoint: 'POST /api/organization/legal-entities', requestPayload: payload, response: { createdAt: body.data?.createdAt, createdBy: body.data?.createdBy }, statusCode: resp.status() });

      expect(resp.status()).toBe(201);
      expect(body.data.createdAt, 'createdAt must be auto-populated').toBeTruthy();
      expect(body.data.createdBy, 'createdBy must be auto-populated').toBeTruthy();

      createdIds.push(body.data.id as number);
    });
  });
});
