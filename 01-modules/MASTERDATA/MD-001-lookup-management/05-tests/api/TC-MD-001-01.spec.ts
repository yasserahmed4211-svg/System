/**
 * G1 — MdMasterLookup: CRUD
 * TC-001 → TC-010
 *
 * Source: registry-masterdata.md v1.1.0 + TC-MASTERDATA.md
 * Layer : Backend API
 * Author: Playwright Expert Agent (auto-generated 2026-04-18)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';
import { validateInsert, validateUpdate } from '../db/db.helper';

const BASE        = 'http://localhost:7272';
const API_LOOKUPS = `${BASE}/api/masterdata/master-lookups`;
const TENANT      = 'default';

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
  const body = await resp.json();
  return (body.data?.accessToken ?? body.accessToken) as string;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G1 — MdMasterLookup: CRUD', () => {
  let ctx:   APIRequestContext;
  let token: string;
  const createdIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);
  });

  test.afterAll(async () => {
    for (const id of createdIds) {
      await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-001 ─────────────────────────────────────────────────────────────────
  test('[TC-001] Create Master Lookup — Success (HTTP 201, key uppercased)', async () => {
    const key     = `VNDR_${uid()}`;
    const payload = { lookupKey: key, lookupName: 'نوع المورد', isActive: true };

    await test.step('POST /api/masterdata/master-lookups', async () => {
      const resp = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-001', endpoint: 'POST /api/masterdata/master-lookups', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Expected HTTP 201').toBe(201);
      expect(body.data, 'Response must contain data').toBeDefined();
      expect(body.data.lookupKey, 'lookupKey must be stored uppercase').toBe(key.toUpperCase());

      createdIds.push(body.data.id);
    });
  });

  // ── TC-002 ─────────────────────────────────────────────────────────────────
  test('[TC-002] Create Master Lookup — lookupKey auto-uppercased on persist', async () => {
    const key     = `vndr_${uid()}`.toLowerCase();
    const payload = { lookupKey: key, lookupName: 'اختبار الحروف الكبيرة', isActive: true };

    await test.step('POST with lowercase key', async () => {
      const resp = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-002', endpoint: 'POST /api/masterdata/master-lookups', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(201);
      expect(body.data.lookupKey).toBe(key.toUpperCase());

      createdIds.push(body.data.id);
    });
  });

  // ── TC-003 ─────────────────────────────────────────────────────────────────
  test('[TC-003] Get Master Lookup by ID — HTTP 200, includes detailCount', async () => {
    // Create a fresh lookup to retrieve
    const key  = `RD_${uid()}`;
    const cr   = await ctx.post(API_LOOKUPS, { data: { lookupKey: key, lookupName: 'قراءة' }, headers: hdrs(token) });
    const crb  = await cr.json();
    const id   = crb.data?.id as number;
    createdIds.push(id);

    await test.step(`GET /api/masterdata/master-lookups/${id}`, async () => {
      const resp = await ctx.get(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-003', endpoint: `GET /api/masterdata/master-lookups/${id}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.id).toBe(id);
      expect(body.data.lookupKey).toBe(key.toUpperCase());
      expect(typeof body.data.detailCount).toBe('number');

      // API-proxy DB validation
      const dbResult = await validateInsert(ctx, API_LOOKUPS, id, hdrs(token));
      expect(dbResult.validated, `DB validation failed: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });

  // ── TC-004 ─────────────────────────────────────────────────────────────────
  test('[TC-004] Get Master Lookup by ID — Non-existent ID returns HTTP 404', async () => {
    const bogusId = 999999999;

    await test.step(`GET /api/masterdata/master-lookups/${bogusId}`, async () => {
      const resp = await ctx.get(`${API_LOOKUPS}/${bogusId}`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-004', endpoint: `GET /api/masterdata/master-lookups/${bogusId}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(404);
    });
  });

  // ── TC-005 ─────────────────────────────────────────────────────────────────
  test('[TC-005] Update Master Lookup — Success; lookupKey remains unchanged', async () => {
    const key      = `UPD_${uid()}`;
    const cr       = await ctx.post(API_LOOKUPS, { data: { lookupKey: key, lookupName: 'قبل التحديث' }, headers: hdrs(token) });
    const crb      = await cr.json();
    const id       = crb.data?.id as number;
    createdIds.push(id);

    const updatePayload = { lookupName: 'نوع المورد المحدث', description: 'وصف جديد' };

    await test.step(`PUT /api/masterdata/master-lookups/${id}`, async () => {
      const resp = await ctx.put(`${API_LOOKUPS}/${id}`, { data: updatePayload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-005', endpoint: `PUT /api/masterdata/master-lookups/${id}`, requestPayload: updatePayload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.lookupName).toBe(updatePayload.lookupName);
      expect(body.data.lookupKey).toBe(key.toUpperCase()); // key must be unchanged

      const dbResult = await validateUpdate(ctx, API_LOOKUPS, id, { lookupName: updatePayload.lookupName }, hdrs(token));
      expect(dbResult.validated, `DB update validation failed: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });

  // ── TC-006 ─────────────────────────────────────────────────────────────────
  test('[TC-006] Update attempt to change lookupKey — silently ignored (key immutable)', async () => {
    const key    = `IMM_${uid()}`;
    const cr     = await ctx.post(API_LOOKUPS, { data: { lookupKey: key, lookupName: 'اختبار ثبات المفتاح' }, headers: hdrs(token) });
    const crb    = await cr.json();
    const id     = crb.data?.id as number;
    createdIds.push(id);

    const payload = { lookupKey: `CHANGED_${uid()}`, lookupName: 'محدث' };

    await test.step('PUT with lookupKey in payload', async () => {
      const resp = await ctx.put(`${API_LOOKUPS}/${id}`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-006', endpoint: `PUT /api/masterdata/master-lookups/${id}`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.lookupKey).toBe(key.toUpperCase()); // original key intact
    });
  });

  // ── TC-007 ─────────────────────────────────────────────────────────────────
  test('[TC-007] Create — missing lookupKey returns HTTP 400', async () => {
    const payload = { lookupName: 'بدون مفتاح', isActive: true };

    await test.step('POST without lookupKey', async () => {
      const resp = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-007', endpoint: 'POST /api/masterdata/master-lookups', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(400);
    });
  });

  // ── TC-008 ─────────────────────────────────────────────────────────────────
  test('[TC-008] Create — missing lookupName returns HTTP 400', async () => {
    const payload = { lookupKey: `NK_${uid()}`, isActive: true };

    await test.step('POST without lookupName', async () => {
      const resp = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-008', endpoint: 'POST /api/masterdata/master-lookups', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(400);
    });
  });

  // ── TC-009 ─────────────────────────────────────────────────────────────────
  test('[TC-009] Create — lookupKey exceeding 50 characters returns HTTP 400', async () => {
    const longKey = 'X'.repeat(51);
    const payload = { lookupKey: longKey, lookupName: 'مفتاح طويل', isActive: true };

    await test.step('POST with 51-char lookupKey', async () => {
      const resp = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-009', endpoint: 'POST /api/masterdata/master-lookups', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(400);
    });
  });

  // ── TC-010 ─────────────────────────────────────────────────────────────────
  test('[TC-010] System fields (createdAt, createdBy) auto-populated on create; updatedAt is null', async () => {
    const key     = `AUDIT_${uid()}`;
    const payload = { lookupKey: key, lookupName: 'حقول النظام', isActive: true };

    await test.step('POST and verify audit fields in response', async () => {
      const resp = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-010', endpoint: 'POST /api/masterdata/master-lookups', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(201);
      expect(body.data.createdAt, 'createdAt must be populated').toBeTruthy();
      expect(body.data.createdBy, 'createdBy must be populated').toBeTruthy();
      expect(body.data.updatedAt, 'updatedAt must be null on initial create').toBeNull();

      createdIds.push(body.data.id);
    });
  });
});
